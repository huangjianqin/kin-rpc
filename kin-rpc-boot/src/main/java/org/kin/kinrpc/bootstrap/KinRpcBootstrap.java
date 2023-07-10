package org.kin.kinrpc.bootstrap;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.registry.RegistryHelper;

import javax.annotation.Nullable;
import java.util.*;
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

    //---------------------------------------------------------------------------config
    /** 应用配置 */
    private ApplicationConfig app;
    /** 全局配置 */
    private final Multimap<Class<?>, Config> configs = MultimapBuilder.hashKeys().linkedListValues().build();
    /**
     * {@link SharableConfig}, {@link SharableConfig#getId()} -> 全局配置
     * 其他, 自定义key -> 全局配置
     */
    private final Map<Class<?>, Map<String, Config>> configMap = new HashMap<>();
    /** 服务所属组 */
    private String group;
    /** 版本号 */
    private String version;
    /** 默认序列化方式 */
    private String serialization;

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
    }

    /**
     * boostrap start
     */
    public void start() {
        if (!state.compareAndSet(INIT_STATE, STARTED_STATE)) {
            return;
        }

        //配置预处理
        initConfig();

        //配置检查
        checkConfig();

        //初始化注册中心
        initRegistries();

        //服务发布
        exportServices();

        //服务引用
        referServices();
    }

    /**
     * 配置预处理
     * 填充全局默认配置, 维护{@link #configMap}映射关系
     * 按优先级进行覆盖 service/reference config -> provider/consumer config -> bootstrap config
     */
    private void initConfig() {
        //初始化bootstrap默认配置
        if (Objects.nonNull(app)) {
            app.initDefaultConfig();
        }

        for (RegistryConfig registryConfig : getConfigs(RegistryConfig.class)) {
            registryConfig.initDefaultConfig();
            putConfig(registryConfig.getId(), registryConfig);
        }

        for (ExecutorConfig executorConfig : getConfigs(ExecutorConfig.class)) {
            executorConfig.initDefaultConfig();
            putConfig(executorConfig.getId(), executorConfig);
        }

        for (ServerConfig serverConfig : getConfigs(ServerConfig.class)) {
            resolveReferConfig(ExecutorConfig.class, serverConfig::getExecutor, serverConfig::executor, SharableConfig::getId);
            serverConfig.initDefaultConfig();

            putConfig(serverConfig.getId(), serverConfig);
        }

        for (ProviderConfig providerConfig : getConfigs(ProviderConfig.class)) {
            String group = providerConfig.getGroup();
            putConfig(Objects.nonNull(group) ? group : "", providerConfig);

            resolveServiceReferConfig(providerConfig);
            providerConfig.initDefaultConfig();
        }

        for (ConsumerConfig consumerConfig : getConfigs(ConsumerConfig.class)) {
            String group = consumerConfig.getGroup();
            putConfig(Objects.nonNull(group) ? group : "", consumerConfig);

            resolveReferenceReferConfig(consumerConfig);
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

        for (ServiceConfig<?> serviceConfig : getConfigs(ServiceConfig.class)) {
            //尝试从bootstrap或provider获取配置
            String group = serviceConfig.getGroup();
            setUpServiceConfig(serviceConfig, getConfig(ProviderConfig.class, Objects.nonNull(group) ? group : ""));
            //初始化service默认配置
            serviceConfig.initDefaultConfig();
        }

        for (ReferenceConfig<?> referenceConfig : getConfigs(ReferenceConfig.class)) {
            //尝试从bootstrap或consumer获取配置
            String group = referenceConfig.getGroup();
            setUpReferenceConfig(referenceConfig, getConfig(ConsumerConfig.class, Objects.nonNull(group) ? group : ""));
            //初始化reference默认配置
            referenceConfig.initDefaultConfig();
        }
    }

    /**
     * 服务配置预处理
     */
    private void setUpServiceConfig(ServiceConfig<?> serviceConfig,
                                    @Nullable ProviderConfig provider) {
        //处理引用配置
        resolveServiceReferConfig(serviceConfig);

        //interface
        setUpConfigIfNotExists(serviceConfig::app, serviceConfig::getApp, provider, ProviderConfig::getApp, this::getApp);
        setUpConfigIfNotExists(serviceConfig::group, serviceConfig::getGroup, provider, ProviderConfig::getGroup, this::getGroup);
        setUpConfigIfNotExists(serviceConfig::version, serviceConfig::getVersion, provider, ProviderConfig::getVersion, this::getVersion);
        setUpConfigIfNotExists(serviceConfig::serialization, serviceConfig::getSerialization, provider, ProviderConfig::getSerialization, this::getSerialization);
        setUpConfigIfNotExists(serviceConfig::registries, serviceConfig::getRegistries, provider, ProviderConfig::getRegistries, this::getRegistries);
        setUpConfigIfNotExists(serviceConfig::servers, serviceConfig::getServers, provider, ProviderConfig::getServers, this::getServers);
        setUpConfigIfNotExists(serviceConfig::filters, serviceConfig::getFilters, provider, ProviderConfig::getFilters);

        //service
        setUpConfigIfNotExists(serviceConfig::servers, serviceConfig::getServers, provider, ProviderConfig::getServers);
        setUpConfigIfNotExists(serviceConfig::executor, serviceConfig::getExecutor, provider, ProviderConfig::getExecutor);
        setUpConfigIfNotExists(serviceConfig::weight, serviceConfig::getWeight, provider, ProviderConfig::getWeight);
        setUpConfigIfNotExists(serviceConfig::bootstrap, serviceConfig::getBootstrap, provider, ProviderConfig::getBootstrap);
        setUpConfigIfNotExists(serviceConfig::delay, serviceConfig::getDelay, provider, ProviderConfig::getDelay);
        setUpConfigIfNotExists(serviceConfig::token, serviceConfig::getToken, provider, ProviderConfig::getToken);

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
    private void setUpReferenceConfig(ReferenceConfig<?> referenceConfig,
                                      @Nullable ConsumerConfig consumer) {
        //处理引用配置
        resolveReferenceReferConfig(referenceConfig);

        //interface
        setUpConfigIfNotExists(referenceConfig::app, referenceConfig::getApp, consumer, ConsumerConfig::getApp, this::getApp);
        setUpConfigIfNotExists(referenceConfig::group, referenceConfig::getGroup, consumer, ConsumerConfig::getGroup, this::getGroup);
        setUpConfigIfNotExists(referenceConfig::version, referenceConfig::getVersion, consumer, ConsumerConfig::getVersion, this::getVersion);
        setUpConfigIfNotExists(referenceConfig::serialization, referenceConfig::getSerialization, consumer, ConsumerConfig::getSerialization, this::getSerialization);
        setUpConfigIfNotExists(referenceConfig::registries, referenceConfig::getRegistries, consumer, ConsumerConfig::getRegistries, this::getRegistries);
        setUpConfigIfNotExists(referenceConfig::filters, referenceConfig::getFilters, consumer, ConsumerConfig::getFilters);

        //reference
        setUpConfigIfNotExists(referenceConfig::cluster, referenceConfig::getCluster, consumer, ConsumerConfig::getCluster);
        setUpConfigIfNotExists(referenceConfig::loadBalance, referenceConfig::getLoadBalance, consumer, ConsumerConfig::getLoadBalance);
        setUpConfigIfNotExists(referenceConfig::router, referenceConfig::getRouter, consumer, ConsumerConfig::getRouter);
        setUpConfigIfNotExists(referenceConfig::generic, referenceConfig::isGeneric, consumer, ConsumerConfig::isGeneric);
        setUpConfigIfNotExists(referenceConfig::ssl, referenceConfig::getSsl, consumer, ConsumerConfig::getSsl);
        setUpConfigIfNotExists(referenceConfig::bootstrap, referenceConfig::getBootstrap, consumer, ConsumerConfig::getBootstrap);
        setUpConfigIfNotExists(referenceConfig::rpcTimeout, referenceConfig::getRpcTimeout, consumer, ConsumerConfig::getRpcTimeout);
        setUpConfigIfNotExists(referenceConfig::retries, referenceConfig::getRetries, consumer, ConsumerConfig::getRetries);
        setUpConfigIfNotExists(referenceConfig::async, referenceConfig::isAsync, consumer, ConsumerConfig::isAsync);
        setUpConfigIfNotExists(referenceConfig::sticky, referenceConfig::isSticky, consumer, ConsumerConfig::isSticky);

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
    private void resolveServiceReferConfig(AbstractServiceConfig<?> serviceConfig) {
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
    private void resolveReferenceReferConfig(AbstractReferenceConfig<?> referenceConfig) {
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
                C referConfig = getConfig(configClass, key);
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
            C referConfig = getConfig(configClass, key);
            if (Objects.isNull(referConfig)) {
                throw new IllegalConfigException(String.format("can not find '%s' with key = '%s'", configClass.getName(), key));
            }

            setter.accept(referConfig);
        }
    }

    /**
     * 尝试设置配置, 按优先级进行覆盖 {@code c1VGetter} ->{@code c2VGetter} ->  {@code c3VGetter}
     *
     * @param setter    config value setter
     * @param c1VGetter service/reference config value getter
     * @param c2        provider/consumer config
     * @param c2VGetter provider/consumer config value getter
     */
    private <T, PC> void setUpConfigIfNotExists(Consumer<T> setter,
                                                Supplier<T> c1VGetter,
                                                @Nullable PC c2,
                                                Function<PC, T> c2VGetter) {
        setUpConfigIfNotExists(setter, c1VGetter, c2, c2VGetter, null);
    }

    /**
     * 尝试设置配置, 按优先级进行覆盖 {@code c1VGetter} ->{@code c2VGetter} ->  {@code c3VGetter}
     *
     * @param setter    config value setter
     * @param c1VGetter service/reference config value getter
     * @param c2        provider/consumer config
     * @param c2VGetter provider/consumer config value getter
     * @param c3VGetter bootstrap config value getter
     */
    private <T, PC> void setUpConfigIfNotExists(Consumer<T> setter,
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
        Set<String> listenHostPort = new HashSet<>();
        for (ServiceConfig<?> serviceConfig : getConfigs(ServiceConfig.class)) {
            serviceConfig.checkValid();
            if (!exportServices.add(serviceConfig.getService())) {
                throw new IllegalConfigException("more than one service config for service '%s'");
            }

            //检查所有server config监听的host:port是否冲突
            for (ServerConfig serverConfig : serviceConfig.getServers()) {
                String ipPort = NetUtils.getIpPort(serverConfig.getHost(), serverConfig.getPort());
                if (!listenHostPort.add(ipPort)) {
                    throw new IllegalConfigException(String.format("listen host and port conflict, '%s'", ipPort));
                }
            }
        }

        Set<String> referServices = new HashSet<>();
        for (ReferenceConfig<?> referenceConfig : getConfigs(ReferenceConfig.class)) {
            referenceConfig.checkValid();
            if (!referServices.add(referenceConfig.getService())) {
                throw new IllegalConfigException("more than one reference config for service '%s'");
            }
        }
    }

    /**
     * 初始化注册中心
     */
    private void initRegistries() {
        List<RegistryConfig> registryConfigs = getConfigs(RegistryConfig.class);
        if (CollectionUtils.isNonEmpty(registryConfigs)) {
            return;
        }

        for (RegistryConfig registryConfig : registryConfigs) {
            RegistryHelper.getRegistry(registryConfig);
        }
    }

    /**
     * 发布服务
     */
    @SuppressWarnings("rawtypes")
    private void exportServices() {
        List<ServiceConfig> serviceConfigs = getConfigs(ServiceConfig.class);
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            return;
        }

        List<ServiceConfig<?>> nonDelayServices = new ArrayList<>();
        for (ServiceConfig<?> serviceConfig : serviceConfigs) {
            long delay = serviceConfig.getDelay();
            if (delay > 0) {
                //先发布延迟发布的服务
                serviceConfig.export();
            } else {
                nonDelayServices.add(serviceConfig);
            }
        }

        //后发布非延迟发布的服务
        for (ServiceConfig<?> serviceConfig : nonDelayServices) {
            serviceConfig.export();
        }
    }

    /**
     * 服务引用
     */
    @SuppressWarnings("rawtypes")
    private void referServices() {
        List<ReferenceConfig> referenceConfigs = getConfigs(ReferenceConfig.class);
        if (CollectionUtils.isEmpty(referenceConfigs)) {
            return;
        }

        for (ReferenceConfig<?> referenceConfig : referenceConfigs) {
            Class<?> interfaceClass = referenceConfig.getInterfaceClass();
            String service = referenceConfig.getService();
            Object proxy = referenceConfig.refer();

            if (!GenericService.class.equals(interfaceClass)) {
                interface2Reference.put(interfaceClass, proxy);
            }
            service2Reference.put(service, proxy);
        }
    }

    /**
     * boostrap destroy
     */
    public void destroy() {
        if (!state.compareAndSet(STARTED_STATE, TERMINATED_STATE)) {
            return;
        }

        for (ReferenceConfig<?> referenceConfig : getConfigs(ReferenceConfig.class)) {
            referenceConfig.unRefer();
        }

        for (ServiceConfig<?> serviceConfig : getConfigs(ServiceConfig.class)) {
            serviceConfig.unExport();
        }

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
                    Thread.currentThread().interrupt();
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
     * 添加配置
     *
     * @param c 配置
     */
    private void addConfig(Config c) {
        Class<? extends Config> configClass = c.getClass();
        configs.put(configClass, c);
    }

    /**
     * 批量添加配置
     *
     * @param cs 配置列表
     */
    private <C extends Config> void addConfigs(Collection<C> cs) {
        for (Config c : cs) {
            addConfig(c);
        }
    }

    /**
     * 返回{@code configClass}类型配置
     *
     * @return 配置列表
     */
    @SuppressWarnings("unchecked")
    private <C extends Config> List<C> getConfigs(Class<C> configClass) {
        return (List<C>) new ArrayList<>(configs.get(configClass));
    }

    /**
     * 返回{@code configClass}类型和{@code key}映射的配置
     *
     * @return 配置
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <C extends Config> C getConfig(Class<C> configClass, String key) {
        return (C) configMap.get(configClass).get(key);
    }

    /**
     * 添加配置映射
     *
     * @param c 配置
     */
    private void putConfig(String key, Config c) {
        Map<String, Config> k2Config = configMap.computeIfAbsent(c.getClass(), k -> new HashMap<>());
        k2Config.put(key, c);
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

    public List<RegistryConfig> getRegistries() {
        return getConfigs(RegistryConfig.class);
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
        addConfigs(registries);
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

    public List<ProviderConfig> getProviders() {
        return getConfigs(ProviderConfig.class);
    }

    public KinRpcBootstrap provider(ProviderConfig provider) {
        return providers(provider);
    }

    public KinRpcBootstrap providers(ProviderConfig... providers) {
        checkBeforeModify();
        return providers(Arrays.asList(providers));
    }

    public KinRpcBootstrap providers(List<ProviderConfig> providers) {
        checkBeforeModify();
        addConfigs(providers);
        return this;
    }

    public List<ConsumerConfig> getConsumers() {
        return getConfigs(ConsumerConfig.class);
    }

    public KinRpcBootstrap consumer(ConsumerConfig consumer) {
        return consumers(consumer);
    }

    public KinRpcBootstrap consumers(ConsumerConfig... consumers) {
        checkBeforeModify();
        return consumers(Arrays.asList(consumers));
    }

    public KinRpcBootstrap consumers(List<ConsumerConfig> consumers) {
        checkBeforeModify();
        addConfigs(consumers);
        return this;
    }

    public KinRpcBootstrap service(ServiceConfig<?> service) {
        checkBeforeModify();
        addConfig(service);
        return this;
    }

    public KinRpcBootstrap reference(ReferenceConfig<?> reference) {
        checkBeforeModify();
        addConfig(reference);
        return this;
    }

    public List<ServerConfig> getServers() {
        return getConfigs(ServerConfig.class);
    }

    public KinRpcBootstrap server(ServerConfig server) {
        checkBeforeModify();
        return servers(Collections.singletonList(server));
    }

    public KinRpcBootstrap servers(ServerConfig... servers) {
        checkBeforeModify();
        return servers(Arrays.asList(servers));
    }

    public KinRpcBootstrap servers(List<ServerConfig> servers) {
        checkBeforeModify();
        addConfigs(servers);
        return this;
    }

    public List<ExecutorConfig> getExecutors() {
        return getConfigs(ExecutorConfig.class);
    }

    public KinRpcBootstrap executor(ExecutorConfig executor) {
        checkBeforeModify();
        return executors(Collections.singletonList(executor));
    }

    public KinRpcBootstrap executors(ExecutorConfig... executors) {
        checkBeforeModify();
        return executors(Arrays.asList(executors));
    }

    public KinRpcBootstrap executors(List<ExecutorConfig> executors) {
        checkBeforeModify();
        addConfigs(executors);
        return this;
    }
}
