package org.kin.kinrpc.bootstrap;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.*;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 多服务, 多服务引用bootstrap, 主要目的是减少服务发布和服务引用的配置工作量
 *
 * @author huangjianqin
 * @date 2023/7/7
 */
public final class KinRpcBootstrap {
    private static final Logger log = LoggerFactory.getLogger(KinRpcBootstrap.class);

    /** 单例 */
    private static KinRpcBootstrap instance;
    /** 初始状态 */
    private static final byte INIT_STATE = 1;
    /** started状态 */
    private static final byte STARTED_STATE = 2;
    /** unExported后状态 */
    private static final byte TERMINATED_STATE = 3;

    /**
     * await lock
     *
     * @see #block()
     */
    private final Lock blockLock = new ReentrantLock();
    /**
     * await condition
     *
     * @see #block()
     */
    private final Condition blockCondition = blockLock.newCondition();
    /** 当前是否已{@link #block()} */
    private boolean block = false;
    /** 状态 */
    private final AtomicInteger state = new AtomicInteger(INIT_STATE);
    /** async export or refer future */
    private List<CompletableFuture<Void>> asyncExportReferFutures = new ArrayList<>();
    /** async export or refer executor */
    private ExecutorService asyncExportReferExecutor;

    //---------------------------------------------------------------------------config
    private final ApplicationConfigManager configManager = ApplicationConfigManager.instance();
    /** 应用配置 */
    private ApplicationConfig app;
    /** 服务所属组 */
    private String group;
    /** 版本号 */
    private String version;
    /** 默认序列化方式 */
    private String serialization;
    /** 标识是否异步export或refer */
    private boolean asyncExportRefer;
    /** 注册的{@link KinRpcBootstrapListener}实例信息 */
    private final List<KinRpcBootstrapListener> kinRpcBootstrapListeners = new ArrayList<>();
    //--------------------------------------------------------------------------------cache
    /**
     * 服务引用缓存
     * key -> 服务唯一标识, value -> 服务引用代理实例
     */
    private final Map<String, Object> service2Reference = new HashMap<>();
    /**
     * 服务引用缓存
     * key -> 服务接口class, value -> 服务引用代理实例
     */
    private final Map<Class<?>, Object> interface2Reference = new HashMap<>();

    /**
     * 返回{@link KinRpcBootstrap}单例
     *
     * @return {@link KinRpcBootstrap}实例
     */
    public static synchronized KinRpcBootstrap instance() {
        if (Objects.isNull(instance)) {
            instance = new KinRpcBootstrap();
        }

        return instance;
    }

    private KinRpcBootstrap() {
        //加载通过spi注册的listener
        List<KinRpcBootstrapListener> kinRpcBootstrapListeners = ExtensionLoader.getExtensions(KinRpcBootstrapListener.class);
        if (CollectionUtils.isNonEmpty(kinRpcBootstrapListeners)) {
            listeners(kinRpcBootstrapListeners);
        }
    }

    /**
     * boostrap start
     */
    public synchronized void start() {
        if (!state.compareAndSet(INIT_STATE, STARTED_STATE)) {
            return;
        }

        //配置预处理
        initConfig();

        //配置检查
        checkConfig();

        //服务发布
        exportServices();

        //服务引用
        referServices();

        if (CollectionUtils.isEmpty(asyncExportReferFutures)) {
            onStarted();
        } else {
            //async wait
            asyncExportReferExecutor
                    .execute(() -> {
                        waitAsyncExportRefer();

                        onStarted();
                    });
        }
    }

    /**
     * 配置预处理
     * 填充全局默认配置
     * 按优先级进行覆盖 service/reference config -> provider/consumer config -> bootstrap config
     */
    private void initConfig() {
        //初始化bootstrap默认配置
        if (Objects.nonNull(app)) {
            app.initDefaultConfig();
        }

        for (ProviderConfig providerConfig : configManager.getConfigs(ProviderConfig.class)) {
            resolveReferConfig(providerConfig);
            providerConfig.initDefaultConfig();
        }

        for (ConsumerConfig consumerConfig : configManager.getConfigs(ConsumerConfig.class)) {
            resolveReferConfig(consumerConfig);
            consumerConfig.initDefaultConfig();
        }

        if (Objects.isNull(group)) {
            group = DefaultConfig.DEFAULT_GROUP;
        }

        if (Objects.isNull(version)) {
            version = DefaultConfig.DEFAULT_VERSION;
        }

        if (Objects.isNull(serialization)) {
            serialization = DefaultConfig.DEFAULT_SERIALIZATION;
        }

        for (ServiceConfig<?> serviceConfig : configManager.getConfigs(ServiceConfig.class)) {
            //尝试从bootstrap或provider获取配置
            String group = serviceConfig.getGroup();
            ProviderConfig providerConfig = configManager.getConfig(ProviderConfig.class, group);
            if (Objects.isNull(providerConfig)) {
                providerConfig = configManager.getConfig(ProviderConfig.class, "");
            }
            initServiceConfig(serviceConfig, providerConfig);
            //初始化service默认配置
            serviceConfig.initDefaultConfig();
        }

        for (ReferenceConfig<?> referenceConfig : configManager.getConfigs(ReferenceConfig.class)) {
            //尝试从bootstrap或consumer获取配置
            String group = referenceConfig.getGroup();
            ConsumerConfig consumerConfig = configManager.getConfig(ConsumerConfig.class, group);
            if (Objects.isNull(consumerConfig)) {
                consumerConfig = configManager.getConfig(ConsumerConfig.class, "");
            }
            initReferenceConfig(referenceConfig, consumerConfig);
            //初始化reference默认配置
            referenceConfig.initDefaultConfig();
        }
    }

    /**
     * 服务配置预处理
     */
    private void initServiceConfig(ServiceConfig<?> serviceConfig,
                                   @Nullable ProviderConfig provider) {
        //处理引用配置
        resolveReferConfig(serviceConfig);

        //interface
        setParentConfigIfNotExists(serviceConfig::app, serviceConfig::getApp, provider, ProviderConfig::getApp, this::getApp);
        setParentConfigIfNotExists(serviceConfig::group, serviceConfig::getGroup, provider, ProviderConfig::getGroup, this::getGroup);
        setParentConfigIfNotExists(serviceConfig::version, serviceConfig::getVersion, provider, ProviderConfig::getVersion, this::getVersion);
        setParentConfigIfNotExists(serviceConfig::serialization, serviceConfig::getSerialization, provider, ProviderConfig::getSerialization, this::getSerialization);
        setParentConfigIfNotExists(serviceConfig::registries, serviceConfig::getRegistries, provider, ProviderConfig::getRegistries, this::getRegistries);
        setParentConfigIfNotExists(serviceConfig::servers, serviceConfig::getServers, provider, ProviderConfig::getServers, this::getServers);
        setParentConfigIfNotExists(serviceConfig::filters, serviceConfig::getFilters, provider, ProviderConfig::getFilters);

        //service
        setParentConfigIfNotExists(serviceConfig::servers, serviceConfig::getServers, provider, ProviderConfig::getServers);
        setParentConfigIfNotExists(serviceConfig::executor, serviceConfig::getExecutor, provider, ProviderConfig::getExecutor);
        setParentConfigIfNotExists(serviceConfig::weight, serviceConfig::getWeight, provider, ProviderConfig::getWeight);
        setParentConfigIfNotExists(serviceConfig::bootstrap, serviceConfig::getBootstrap, provider, ProviderConfig::getBootstrap);
        setParentConfigIfNotExists(serviceConfig::delay, serviceConfig::getDelay, provider, ProviderConfig::getDelay);
        setParentConfigIfNotExists(serviceConfig::token, serviceConfig::getToken, provider, ProviderConfig::getToken);
        setParentConfigIfNotExists(serviceConfig::exportAsync, serviceConfig::getExportAsync, provider, ProviderConfig::getExportAsync);

        //attachment
        if (Objects.nonNull(provider)) {
            AttachmentMap attachmentMap = new AttachmentMap(provider.attachments());
            //overwrite
            attachmentMap.attachMany(serviceConfig.attachments());
            serviceConfig.attachMany(attachmentMap);
        }
    }

    /**
     * 服务引用配置预处理
     */
    private void initReferenceConfig(ReferenceConfig<?> referenceConfig,
                                     @Nullable ConsumerConfig consumer) {
        //处理引用配置
        resolveReferConfig(referenceConfig);

        //interface
        setParentConfigIfNotExists(referenceConfig::app, referenceConfig::getApp, consumer, ConsumerConfig::getApp, this::getApp);
        setParentConfigIfNotExists(referenceConfig::group, referenceConfig::getGroup, consumer, ConsumerConfig::getGroup, this::getGroup);
        setParentConfigIfNotExists(referenceConfig::version, referenceConfig::getVersion, consumer, ConsumerConfig::getVersion, this::getVersion);
        setParentConfigIfNotExists(referenceConfig::serialization, referenceConfig::getSerialization, consumer, ConsumerConfig::getSerialization, this::getSerialization);
        setParentConfigIfNotExists(referenceConfig::registries, referenceConfig::getRegistries, consumer, ConsumerConfig::getRegistries, this::getRegistries);
        setParentConfigIfNotExists(referenceConfig::filters, referenceConfig::getFilters, consumer, ConsumerConfig::getFilters);

        //reference
        setParentConfigIfNotExists(referenceConfig::cluster, referenceConfig::getCluster, consumer, ConsumerConfig::getCluster);
        setParentConfigIfNotExists(referenceConfig::loadBalance, referenceConfig::getLoadBalance, consumer, ConsumerConfig::getLoadBalance);
        setParentConfigIfNotExists(referenceConfig::router, referenceConfig::getRouter, consumer, ConsumerConfig::getRouter);
        setParentConfigIfNotExists(referenceConfig::generic, referenceConfig::getGeneric, consumer, ConsumerConfig::getGeneric);
        setParentConfigIfNotExists(referenceConfig::ssl, referenceConfig::getSsl, consumer, ConsumerConfig::getSsl);
        setParentConfigIfNotExists(referenceConfig::bootstrap, referenceConfig::getBootstrap, consumer, ConsumerConfig::getBootstrap);
        setParentConfigIfNotExists(referenceConfig::provideBy, referenceConfig::getProvideBy, consumer, ConsumerConfig::getProvideBy);

        //method
        setParentConfigIfNotExists(referenceConfig::rpcTimeout, referenceConfig::getRpcTimeout, consumer, ConsumerConfig::getRpcTimeout);
        setParentConfigIfNotExists(referenceConfig::retries, referenceConfig::getRetries, consumer, ConsumerConfig::getRetries);
        setParentConfigIfNotExists(referenceConfig::async, referenceConfig::getAsync, consumer, ConsumerConfig::getAsync);
        setParentConfigIfNotExists(referenceConfig::sticky, referenceConfig::getSticky, consumer, ConsumerConfig::getSticky);
        setParentConfigIfNotExists(referenceConfig::cache, referenceConfig::getCache, consumer, ConsumerConfig::getCache);
        setParentConfigIfNotExists(referenceConfig::validation, referenceConfig::getValidation, consumer, ConsumerConfig::getValidation);

        //attachment
        if (Objects.nonNull(consumer)) {
            AttachmentMap attachmentMap = new AttachmentMap(consumer.attachments());
            //overwrite
            attachmentMap.attachMany(referenceConfig.attachments());
            referenceConfig.attachMany(attachmentMap);
        }
    }

    /**
     * 处理服务配置中的引用配置
     *
     * @param serviceConfig 服务配置
     */
    private void resolveReferConfig(AbstractServiceConfig<?> serviceConfig) {
        resolveReferConfigs(RegistryConfig.class, serviceConfig::getRegistries, serviceConfig::setRegistries, SharableConfig::getId);
        resolveReferConfigs(ServerConfig.class, serviceConfig::getServers, serviceConfig::setServers, SharableConfig::getId);
        resolveReferConfig(ExecutorConfig.class, serviceConfig::getExecutor, serviceConfig::executor, SharableConfig::getId);
        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            resolveReferConfig(ExecutorConfig.class, serverConfig::getExecutor, serverConfig::executor, SharableConfig::getId);
        }
    }

    /**
     * 处理服务引用配置中的引用配置
     *
     * @param referenceConfig 服务引用配置
     */
    private void resolveReferConfig(AbstractReferenceConfig<?> referenceConfig) {
        resolveReferConfigs(RegistryConfig.class, referenceConfig::getRegistries, referenceConfig::setRegistries, SharableConfig::getId);
    }

    /**
     * 批量替换引用配置
     */
    private <C extends SharableConfig<C>> void resolveReferConfigs(Class<C> configClass,
                                                                   Supplier<List<C>> getter,
                                                                   Consumer<List<C>> setter,
                                                                   Function<C, String> keyGetter) {
        List<C> configs = getter.get();
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }

        List<C> finalConfigs = new ArrayList<>(configs.size());
        for (C config : configs) {
            String id = config.getId();
            if (StringUtils.isBlank(id)) {
                finalConfigs.add(config);
            } else {
                String key = keyGetter.apply(config);
                C referConfig = configManager.getConfig(configClass, key);
                if (Objects.isNull(referConfig)) {
                    throw new IllegalConfigException(String.format("can not find '%s' with key = '%s'", configClass.getName(), key));
                }

                finalConfigs.add(referConfig);
            }
        }

        setter.accept(finalConfigs);
    }

    /**
     * 替换单个引用配置
     */
    private <C extends SharableConfig<C>> void resolveReferConfig(Class<C> configClass,
                                                                  Supplier<C> getter,
                                                                  Consumer<C> setter,
                                                                  Function<C, String> keyGetter) {
        C config = getter.get();
        if (Objects.isNull(config)) {
            return;
        }

        String id = config.getId();
        if (StringUtils.isNotBlank(id)) {
            String key = keyGetter.apply(config);
            C referConfig = configManager.getConfig(configClass, key);
            if (Objects.isNull(referConfig)) {
                throw new IllegalConfigException(String.format("can not find '%s' with key = '%s'", configClass.getName(), key));
            }

            setter.accept(referConfig);
        }
    }

    /**
     * 如果没有配置, 则使用父配置
     * 继承链是{@code c1VGetter} ->{@code c2VGetter} ->  {@code c3VGetter}
     *
     * @param setter    config value setter
     * @param c1VGetter service/reference config value getter
     * @param c2        provider/consumer config
     * @param c2VGetter provider/consumer config value getter
     */
    private <T, PC> void setParentConfigIfNotExists(Consumer<T> setter,
                                                    Supplier<T> c1VGetter,
                                                    @Nullable PC c2,
                                                    Function<PC, T> c2VGetter) {
        setParentConfigIfNotExists(setter, c1VGetter, c2, c2VGetter, null);
    }

    /**
     * 如果没有配置, 则使用父配置
     * 继承链是{@code c1VGetter} ->{@code c2VGetter} ->  {@code c3VGetter}
     *
     * @param setter    config value setter
     * @param c1VGetter service/reference config value getter
     * @param c2        provider/consumer config
     * @param c2VGetter provider/consumer config value getter
     * @param c3VGetter bootstrap config value getter
     */
    private <T, PC> void setParentConfigIfNotExists(Consumer<T> setter,
                                                    Supplier<T> c1VGetter,
                                                    @Nullable PC c2,
                                                    Function<PC, T> c2VGetter,
                                                    @Nullable Supplier<T> c3VGetter) {
        T c1V = c1VGetter.get();
        if (isNonNull(c1V)) {
            return;
        }

        //没有c1V
        T c2V = Objects.nonNull(c2) ? c2VGetter.apply(c2) : null;
        if (isNonNull(c2V)) {
            setter.accept(c2V);
            return;
        }

        //没有c2V
        T c3V = Objects.nonNull(c3VGetter) ? c3VGetter.get() : null;
        if (isNonNull(c3V)) {
            setter.accept(c3V);
            return;
        }
    }

    /**
     * 判断{@code obj}是否为null, 如果{@code obj}是集合或map, 判断是否null或空
     *
     * @param obj 实例
     * @return true表示{@code obj}为null, 空集合或者空map
     */
    private boolean isNonNull(Object obj) {
        if (Objects.isNull(obj)) {
            return false;
        }

        if (obj.getClass().isArray() && CollectionUtils.isEmpty(((Object[]) obj))) {
            return false;
        }

        if (obj instanceof Collection && CollectionUtils.isEmpty(((Collection<?>) obj))) {
            return false;
        }

        if (obj instanceof Map && CollectionUtils.isEmpty(((Map<?, ?>) obj))) {
            return false;
        }

        return true;
    }

    /**
     * 配置检查
     */
    private void checkConfig() {
        Set<String> exportServices = new HashSet<>();
        Map<String, String> listenHostPort2Protocol = new HashMap<>();
        Multimap<String, String> listenHostPort2Services = MultimapBuilder.hashKeys().linkedHashSetValues().build();

        for (ServiceConfig<?> serviceConfig : configManager.getConfigs(ServiceConfig.class)) {
            serviceConfig.checkValid();
            String service = serviceConfig.getService();
            if (!exportServices.add(service)) {
                throw new IllegalConfigException(String.format("more than one service config for service '%s'", service));
            }

            //检查所有server config监听的host:port是否冲突
            for (ServerConfig serverConfig : serviceConfig.getServers()) {
                String protocol = serverConfig.getProtocol();
                String ipPort = NetUtils.getIpPort(serverConfig.getHost(), serverConfig.getPort());

                if (listenHostPort2Protocol.containsKey(ipPort) &&
                        !protocol.equals(listenHostPort2Protocol.get(ipPort))) {
                    throw new IllegalConfigException(String.format("host and port conflict, different protocol listen on same host and port, '%s'", ipPort));
                }

                if (listenHostPort2Services.containsKey(ipPort) &&
                        listenHostPort2Services.get(ipPort).contains(service)) {
                    //一个server仅允许暴露同一服务一次
                    throw new IllegalConfigException(String.format("more than one service '%s' export on '%s'", service, ipPort));
                }

                listenHostPort2Protocol.put(ipPort, protocol);
                listenHostPort2Services.put(ipPort, service);
            }
        }

        Set<String> referServices = new HashSet<>();
        for (ReferenceConfig<?> referenceConfig : configManager.getConfigs(ReferenceConfig.class)) {
            referenceConfig.checkValid();
            String service = referenceConfig.getService();
            if (!referServices.add(service)) {
                throw new IllegalConfigException(String.format("more than one reference config for service '%s'", service));
            }

            //是否配置服务发现注册中心
            boolean discovery = false;
            for (RegistryConfig registry : referenceConfig.getRegistries()) {
                if (RegistryType.DIRECT.getName().equals(registry.getType())) {
                    continue;
                }

                discovery = true;
                break;
            }

            if (discovery && StringUtils.isBlank(referenceConfig.getProvideBy())) {
                throw new IllegalConfigException("reference provideBy must be not blank");
            }
        }
    }

    /**
     * 发布服务
     */
    @SuppressWarnings("rawtypes")
    private void exportServices() {
        Collection<ServiceConfig> serviceConfigs = configManager.getConfigs(ServiceConfig.class);
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            return;
        }

        List<ServiceConfig<?>> nonDelayServices = new ArrayList<>();
        for (ServiceConfig<?> serviceConfig : serviceConfigs) {
            long delay = serviceConfig.getDelay();
            if (delay > 0) {
                //先发布延迟发布的服务
                exportService(serviceConfig);
            } else {
                nonDelayServices.add(serviceConfig);
            }
        }

        //后发布非延迟发布的服务
        for (ServiceConfig<?> serviceConfig : nonDelayServices) {
            exportService(serviceConfig);
        }
    }

    /**
     * 发布服务
     */
    private void exportService(ServiceConfig<?> serviceConfig) {
        if (isAsyncExportRefer()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(serviceConfig::export, getAsyncExportReferExecutor());
            asyncExportReferFutures.add(future);
        } else {
            serviceConfig.export();
        }
    }

    /**
     * 服务引用
     */
    @SuppressWarnings("rawtypes")
    private void referServices() {
        Collection<ReferenceConfig> referenceConfigs = configManager.getConfigs(ReferenceConfig.class);
        if (CollectionUtils.isEmpty(referenceConfigs)) {
            return;
        }

        for (ReferenceConfig<?> referenceConfig : referenceConfigs) {
            if (isAsyncExportRefer()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> referService(referenceConfig), getAsyncExportReferExecutor());
                asyncExportReferFutures.add(future);
            } else {
                referService(referenceConfig);
            }
        }
    }

    /**
     * bootstrap started
     */
    private void onStarted() {
        for (KinRpcBootstrapListener listener : kinRpcBootstrapListeners) {
            try {
                listener.onStarted(this);
            } catch (Exception e) {
                log.error("KinRpcBootstrapListener#onStarted fail", e);
            }
        }

        if (Objects.nonNull(asyncExportReferExecutor)) {
            asyncExportReferExecutor.shutdown();
            asyncExportReferExecutor = null;
        }
    }

    /**
     * 服务引用
     */
    private void referService(ReferenceConfig<?> referenceConfig) {
        Class<?> interfaceClass = referenceConfig.getInterfaceClass();
        String service = referenceConfig.getService();

        Object proxy = referenceConfig.refer();

        if (!GenericService.class.equals(interfaceClass)) {
            interface2Reference.put(interfaceClass, proxy);
        }
        service2Reference.put(service, proxy);
    }

    /**
     * boostrap destroy
     */
    public synchronized void destroy() {
        if (!state.compareAndSet(STARTED_STATE, TERMINATED_STATE)) {
            return;
        }

        for (ReferenceConfig<?> referenceConfig : configManager.getConfigs(ReferenceConfig.class)) {
            referenceConfig.unRefer();
        }

        for (ServiceConfig<?> serviceConfig : configManager.getConfigs(ServiceConfig.class)) {
            serviceConfig.unExport();
        }

        for (KinRpcBootstrapListener listener : kinRpcBootstrapListeners) {
            try {
                listener.onDestroyed(this);
            } catch (Exception e) {
                log.error("KinRpcBootstrapListener#onDestroyed fail", e);
            }
        }

        ApplicationGuardian.instance().destroy();

        //notify block
        blockLock.lock();
        try {
            if (block) {
                block = false;
                blockCondition.notifyAll();
            }
        } finally {
            blockLock.unlock();
        }
    }

    /**
     * block current thread, and never release
     */
    public void block() {
        checkTerminated();
        blockLock.lock();
        try {
            while (!block) {
                try {
                    block = true;
                    blockCondition.await();
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        } finally {
            blockLock.unlock();
        }
    }

    /**
     * 根据服务唯一标识获取服务引用代理实例
     *
     * @param service 服务唯一标识
     * @param <T>     服务接口
     * @return 服务引用代理实例
     */
    @SuppressWarnings("unchecked")
    public <T> T reference(String service) {
        checkTerminated();

        if (!service2Reference.containsKey(service)) {
            throw new IllegalArgumentException(String.format("can not find service reference for service '%s'", service));
        }
        return (T) service2Reference.get(service);
    }

    /**
     * 根据服务接口获取服务引用代理实例
     *
     * @param interfaceClass 服务接口
     * @param <T>            服务接口
     * @return 服务引用代理实例
     */
    @SuppressWarnings("unchecked")
    public <T> T reference(Class<?> interfaceClass) {
        checkTerminated();

        if (GenericService.class.equals(interfaceClass)) {
            throw new IllegalArgumentException("please use KinRpcBootstrap#reference(String) method to find generic service reference");
        }

        if (!interface2Reference.containsKey(interfaceClass)) {
            throw new IllegalArgumentException(String.format("can not find service reference for service interface '%s'", interfaceClass));
        }
        return (T) interface2Reference.get(interfaceClass);
    }

    /**
     * 修改配置前的检查
     */
    private void checkBeforeModify() {
        if (state.get() > INIT_STATE) {
            throw new IllegalStateException("please modify bootstrap before start");
        }
    }

    /**
     * 检查bootstrap状态
     */
    private void checkTerminated() {
        if (isTerminated()) {
            throw new IllegalStateException("bootstrap is terminated");
        }
    }

    /**
     * 判断当前状态是否{@link #TERMINATED_STATE}
     *
     * @return true标识已terminated
     */
    private boolean isTerminated() {
        return state.get() == TERMINATED_STATE;
    }

    /**
     * 返回async export or refer executor
     *
     * @return
     */
    private ExecutorService getAsyncExportReferExecutor() {
        if (Objects.isNull(asyncExportReferExecutor)) {
            asyncExportReferExecutor = ThreadPoolUtils.newThreadPool("kinrpc-asyncExportRefer-", false,
                    SysUtils.DOUBLE_CPU, SysUtils.DOUBLE_CPU, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new SimpleThreadFactory("kinrpc-asyncExportRefer-", true));
        }
        return asyncExportReferExecutor;
    }

    /**
     * 等待async export or refer结束
     */
    private void waitAsyncExportRefer() {
        if (CollectionUtils.isEmpty(asyncExportReferFutures)) {
            return;
        }

        try {
            try {
                CompletableFuture.allOf(asyncExportReferFutures.toArray(new CompletableFuture<?>[]{}))
                        .get();
            } catch (Throwable e) {
                if (e instanceof ExecutionException) {
                    e = e.getCause();
                }
                log.error("async export or refer error", e);
            }
        } finally {
            asyncExportReferFutures.clear();
            asyncExportReferFutures = null;
            log.info("async export or refer finished");
        }
    }

    //setter && getter
    public ApplicationConfig getApp() {
        return app;
    }

    public KinRpcBootstrap app(String app) {
        checkBeforeModify();
        return app(ApplicationConfig.create(app));
    }

    public KinRpcBootstrap app(ApplicationConfig app) {
        checkBeforeModify();
        this.app = app;
        return this;
    }

    public Collection<RegistryConfig> getRegistries() {
        return configManager.getConfigs(RegistryConfig.class);
    }

    public KinRpcBootstrap registry(RegistryConfig registry) {
        checkBeforeModify();
        return registries(Collections.singletonList(registry));
    }

    public KinRpcBootstrap registries(RegistryConfig... registries) {
        checkBeforeModify();
        return registries(Arrays.asList(registries));
    }

    public KinRpcBootstrap registries(List<RegistryConfig> registries) {
        checkBeforeModify();
        configManager.addConfigs(registries, RegistryConfig::getId);
        return this;
    }

    public String getGroup() {
        return group;
    }

    public KinRpcBootstrap group(String group) {
        checkBeforeModify();
        this.group = group;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public KinRpcBootstrap version(String version) {
        checkBeforeModify();
        this.version = version;
        return this;
    }

    public String getSerialization() {
        return serialization;
    }

    public KinRpcBootstrap serialization(String serialization) {
        checkBeforeModify();
        this.serialization = serialization;
        return this;
    }

    public Collection<ProviderConfig> getProviders() {
        return configManager.getConfigs(ProviderConfig.class);
    }

    public KinRpcBootstrap provider(ProviderConfig provider) {
        return providers(provider);
    }

    public KinRpcBootstrap providers(ProviderConfig... providers) {
        checkBeforeModify();
        return providers(Arrays.asList(providers));
    }

    public KinRpcBootstrap providers(Collection<ProviderConfig> providers) {
        checkBeforeModify();
        configManager.addConfigs(providers,
                pc -> Objects.nonNull(pc.getGroup()) ? pc.getGroup() : "");
        return this;
    }

    public Collection<ConsumerConfig> getConsumers() {
        return configManager.getConfigs(ConsumerConfig.class);
    }

    public KinRpcBootstrap consumer(ConsumerConfig consumer) {
        return consumers(consumer);
    }

    public KinRpcBootstrap consumers(ConsumerConfig... consumers) {
        checkBeforeModify();
        return consumers(Arrays.asList(consumers));
    }

    public KinRpcBootstrap consumers(Collection<ConsumerConfig> consumers) {
        checkBeforeModify();
        configManager.addConfigs(consumers,
                cc -> Objects.nonNull(cc.getGroup()) ? cc.getGroup() : "");
        return this;
    }

    public KinRpcBootstrap service(ServiceConfig<?> service) {
        return services(service);
    }

    public KinRpcBootstrap services(ServiceConfig<?>... services) {
        return services(Arrays.asList(services));
    }

    @SuppressWarnings("rawtypes")
    public KinRpcBootstrap services(Collection<ServiceConfig> services) {
        checkBeforeModify();
        for (ServiceConfig<?> service : services) {
            configManager.addConfig(service);
        }
        return this;
    }

    public KinRpcBootstrap reference(ReferenceConfig<?> reference) {
        return references(reference);
    }

    public KinRpcBootstrap references(ReferenceConfig<?>... references) {
        return references(Arrays.asList(references));
    }

    @SuppressWarnings("rawtypes")
    public KinRpcBootstrap references(Collection<ReferenceConfig> references) {
        checkBeforeModify();
        for (ReferenceConfig<?> reference : references) {
            configManager.addConfig(reference);
        }
        return this;
    }

    public Collection<ServerConfig> getServers() {
        return configManager.getConfigs(ServerConfig.class);
    }

    public KinRpcBootstrap server(ServerConfig server) {
        checkBeforeModify();
        return servers(Collections.singletonList(server));
    }

    public KinRpcBootstrap servers(ServerConfig... servers) {
        checkBeforeModify();
        return servers(Arrays.asList(servers));
    }

    public KinRpcBootstrap servers(Collection<ServerConfig> servers) {
        checkBeforeModify();
        configManager.addConfigs(servers, ServerConfig::getId);
        return this;
    }

    public Collection<ExecutorConfig> getExecutors() {
        return configManager.getConfigs(ExecutorConfig.class);
    }

    public KinRpcBootstrap executor(ExecutorConfig executor) {
        checkBeforeModify();
        return executors(Collections.singletonList(executor));
    }

    public KinRpcBootstrap executors(ExecutorConfig... executors) {
        checkBeforeModify();
        return executors(Arrays.asList(executors));
    }

    public KinRpcBootstrap executors(Collection<ExecutorConfig> executors) {
        checkBeforeModify();
        configManager.addConfigs(executors, ExecutorConfig::getId);
        return this;
    }

    public boolean isAsyncExportRefer() {
        return asyncExportRefer;
    }

    public KinRpcBootstrap asyncExportRefer() {
        this.asyncExportRefer = true;
        return this;
    }

    public KinRpcBootstrap listeners(KinRpcBootstrapListener... listeners) {
        return listeners(Arrays.asList(listeners));
    }

    public KinRpcBootstrap listeners(Collection<KinRpcBootstrapListener> listeners) {
        kinRpcBootstrapListeners.addAll(listeners);
        return this;
    }
}
