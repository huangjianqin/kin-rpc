package org.kin.kinrpc.bootstrap;

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
    /** 注册中心配置 */
    private final List<RegistryConfig> registries = new ArrayList<>();
    /** 服务所属组 */
    private String group;
    /** 版本号 */
    private String version;
    /** 默认序列化方式 */
    private String serialization;
    /** provider config */
    private final Map<String, ProviderConfig> providerMap = new HashMap<>();
    /** consumer config */
    private final Map<String, ConsumerConfig> consumerMap = new HashMap<>();
    /** 服务配置 */
    private final List<ServiceConfig<?>> services = new ArrayList<>();
    /** 服务引用配置 */
    private final List<ReferenceConfig<?>> references = new ArrayList<>();

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
        setUpConfig();

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
     * 配置预处理, 按优先级进行覆盖 service/reference config -> provider/consumer config -> bootstrap config
     */
    private void setUpConfig() {
        //初始化bootstrap默认配置
        initDefaultConfig();

        //初始化provider默认配置
        for (ProviderConfig providerConfig : providerMap.values()) {
            providerConfig.initDefaultConfig();
        }

        //初始化consumer默认配置
        for (ConsumerConfig consumerConfig : consumerMap.values()) {
            consumerConfig.initDefaultConfig();
        }

        for (ServiceConfig<?> serviceConfig : services) {
            //尝试从bootstrap或provider获取配置
            String group = serviceConfig.getGroup();
            setUpServiceConfig(serviceConfig, providerMap.get(Objects.nonNull(group) ? group : ""));
            //初始化service默认配置
            serviceConfig.initDefaultConfig();
        }

        for (ReferenceConfig<?> referenceConfig : references) {
            //尝试从bootstrap或consumer获取配置
            String group = referenceConfig.getGroup();
            setUpReferenceConfig(referenceConfig, consumerMap.get(Objects.nonNull(group) ? group : ""));
            //初始化reference默认配置
            referenceConfig.initDefaultConfig();
        }
    }

    /**
     * 服务配置预处理
     */
    private void setUpServiceConfig(ServiceConfig<?> service,
                                    @Nullable ProviderConfig provider) {
        //interface
        setUpConfigIfNotExists(service::app, service::getApp, provider, ProviderConfig::getApp, this::getApp);
        setUpConfigIfNotExists(service::group, service::getGroup, provider, ProviderConfig::getGroup, this::getGroup);
        setUpConfigIfNotExists(service::version, service::getVersion, provider, ProviderConfig::getVersion, this::getVersion);
        setUpConfigIfNotExists(service::serialization, service::getSerialization, provider, ProviderConfig::getSerialization, this::getSerialization);
        setUpConfigIfNotExists(service::registries, service::getRegistries, provider, ProviderConfig::getRegistries, this::getRegistries);
        setUpConfigIfNotExists(service::filters, service::getFilters, provider, ProviderConfig::getFilters);

        //service
        setUpConfigIfNotExists(service::servers, service::getServers, provider, ProviderConfig::getServers);
        setUpConfigIfNotExists(service::executor, service::getExecutor, provider, ProviderConfig::getExecutor);
        setUpConfigIfNotExists(service::weight, service::getWeight, provider, ProviderConfig::getWeight);
        setUpConfigIfNotExists(service::bootstrap, service::getBootstrap, provider, ProviderConfig::getBootstrap);
        setUpConfigIfNotExists(service::delay, service::getDelay, provider, ProviderConfig::getDelay);
        setUpConfigIfNotExists(service::token, service::getToken, provider, ProviderConfig::getToken);

        //attachment
        if (Objects.nonNull(provider)) {
            AttachmentMap attachmentMap = new AttachmentMap(provider.attachments());
            //overwrite
            attachmentMap.attachMany(service.attachments());
            service.attachMany(attachmentMap);
        }
    }

    /**
     * 服务引用配置预处理
     */
    private void setUpReferenceConfig(ReferenceConfig<?> reference,
                                      @Nullable ConsumerConfig consumer) {
        //interface
        setUpConfigIfNotExists(reference::app, reference::getApp, consumer, ConsumerConfig::getApp, this::getApp);
        setUpConfigIfNotExists(reference::group, reference::getGroup, consumer, ConsumerConfig::getGroup, this::getGroup);
        setUpConfigIfNotExists(reference::version, reference::getVersion, consumer, ConsumerConfig::getVersion, this::getVersion);
        setUpConfigIfNotExists(reference::serialization, reference::getSerialization, consumer, ConsumerConfig::getSerialization, this::getSerialization);
        setUpConfigIfNotExists(reference::registries, reference::getRegistries, consumer, ConsumerConfig::getRegistries, this::getRegistries);
        setUpConfigIfNotExists(reference::filters, reference::getFilters, consumer, ConsumerConfig::getFilters);

        //reference
        setUpConfigIfNotExists(reference::cluster, reference::getCluster, consumer, ConsumerConfig::getCluster);
        setUpConfigIfNotExists(reference::loadBalance, reference::getLoadBalance, consumer, ConsumerConfig::getLoadBalance);
        setUpConfigIfNotExists(reference::router, reference::getRouter, consumer, ConsumerConfig::getRouter);
        setUpConfigIfNotExists(reference::generic, reference::isGeneric, consumer, ConsumerConfig::isGeneric);
        setUpConfigIfNotExists(reference::ssl, reference::getSsl, consumer, ConsumerConfig::getSsl);
        setUpConfigIfNotExists(reference::bootstrap, reference::getBootstrap, consumer, ConsumerConfig::getBootstrap);
        setUpConfigIfNotExists(reference::rpcTimeout, reference::getRpcTimeout, consumer, ConsumerConfig::getRpcTimeout);
        setUpConfigIfNotExists(reference::retries, reference::getRetries, consumer, ConsumerConfig::getRetries);
        setUpConfigIfNotExists(reference::async, reference::isAsync, consumer, ConsumerConfig::isAsync);
        setUpConfigIfNotExists(reference::sticky, reference::isSticky, consumer, ConsumerConfig::isSticky);

        //attachment
        if (Objects.nonNull(consumer)) {
            AttachmentMap attachmentMap = new AttachmentMap(consumer.attachments());
            //overwrite
            attachmentMap.attachMany(reference.attachments());
            reference.attachMany(attachmentMap);
        }
    }

    /**
     * 当{@code t}非null时, 值用{@code func}并返回R, 否则返回null
     */
    private <T, R> R getIfNonNull(T t, Function<T, R> func) {
        if (Objects.nonNull(t)) {
            return func.apply(t);
        }

        return null;
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
        if (!isNullOrEmpty(c1V)) {
            return;
        }

        //没有c1V
        T c2V = Objects.nonNull(c2) ? c2VGetter.apply(c2) : null;
        if (!isNullOrEmpty(c2V)) {
            setter.accept(c2V);
            return;
        }

        //没有c2V
        T c3V = Objects.nonNull(c3VGetter) ? c3VGetter.get() : null;
        if (!isNullOrEmpty(c3V)) {
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
    private boolean isNullOrEmpty(Object obj) {
        if (Objects.isNull(obj)) {
            return true;
        }

        if (obj.getClass().isArray() && CollectionUtils.isEmpty(((Object[]) obj))) {
            return true;
        }

        if (obj instanceof Collection && CollectionUtils.isEmpty(((Collection<?>) obj))) {
            return true;
        }

        if (obj instanceof Map && CollectionUtils.isEmpty(((Map<?, ?>) obj))) {
            return true;
        }

        return false;
    }

    /**
     * 填充bootstrap默认配置
     */
    private void initDefaultConfig() {
        if (Objects.nonNull(app)) {
            app.initDefaultConfig();
        }

        for (RegistryConfig registry : registries) {
            registry.initDefaultConfig();
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
    }

    /**
     * 配置检查
     */
    private void checkConfig() {
        Set<String> exportServices = new HashSet<>();
        Set<String> listenHostPort = new HashSet<>();
        for (ServiceConfig<?> serviceConfig : services) {
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
        for (ReferenceConfig<?> referenceConfig : references) {
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
        if (CollectionUtils.isNonEmpty(registries)) {
            return;
        }

        for (RegistryConfig registryConfig : registries) {
            RegistryHelper.getRegistry(registryConfig);
        }
    }

    /**
     * 发布服务
     */
    private void exportServices() {
        if (CollectionUtils.isEmpty(services)) {
            return;
        }

        List<ServiceConfig<?>> nonDelayServices = new ArrayList<>();
        for (ServiceConfig<?> serviceConfig : services) {
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
    private void referServices() {
        if (CollectionUtils.isEmpty(references)) {
            return;
        }

        for (ReferenceConfig<?> referenceConfig : references) {
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

        for (ReferenceConfig<?> referenceConfig : references) {
            referenceConfig.unRefer();
        }

        for (ServiceConfig<?> serviceConfig : services) {
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
        return registries;
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
        this.registries.addAll(registries);
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

    public ProviderConfig getProvider() {
        return getProvider("");
    }

    public ProviderConfig getProvider(String group) {
        return providerMap.get(group);
    }

    public KinRpcBootstrap provider(ProviderConfig provider) {
        checkBeforeModify();
        String group = provider.getGroup();
        if (StringUtils.isBlank(group)) {
            group = "";
        }
        providerMap.put(group, provider);
        return this;
    }

    public ConsumerConfig getConsumer() {
        return getConsumer("");
    }

    public ConsumerConfig getConsumer(String group) {
        return consumerMap.get(group);
    }

    public KinRpcBootstrap consumer(ConsumerConfig consumer) {
        checkBeforeModify();
        String group = consumer.getGroup();
        if (StringUtils.isBlank(group)) {
            group = "";
        }
        consumerMap.put(group, consumer);
        return this;
    }

    public KinRpcBootstrap service(ServiceConfig<?> service) {
        checkBeforeModify();
        services.add(service);
        return this;
    }

    public KinRpcBootstrap reference(ReferenceConfig<?> reference) {
        checkBeforeModify();
        references.add(reference);
        return this;
    }
}
