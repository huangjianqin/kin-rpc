package org.kin.kinrpc.config;


import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;
import org.kin.kinrpc.serializer.Serializers;
import org.kin.kinrpc.transport.ProtocolType;
import org.kin.transport.netty.CompressionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServiceConfig<T> extends AbstractConfig {
    /** 应用配置 */
    private ApplicationConfig applicationConfig = new ApplicationConfig();
    /** 网络配置 */
    private ServerConfig serverConfig = ServerConfig.DEFAULT;
    /** 注册中心配置 */
    private AbstractRegistryConfig registryConfig;
    /** 服务实例 */
    private T ref;
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务名 */
    private String serviceName;
    /** 序列化类型, 在kinrpc协议下生效 */
    private int serialize = SerializerType.KRYO.getCode();
    /** 服务调用类型(invoker收到请求后, 用哪种方式调用服务) */
    private InvokeType invokeType = InvokeType.JAVASSIST;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 压缩类型, 在kinrpc协议下生效 todo 考虑降低耦合性 */
    private CompressionType compressionType = CompressionType.NONE;
    /** 默认支持并发执行 */
    private boolean parallelism = true;
    /** 流控, 每秒最多处理多少个request */
    private int rate = Constants.PROVIDER_REQUEST_THRESHOLD;
    /** 协议类型 */
    private ProtocolType protocolType = ProtocolType.KinRpc;

    /** 唯一url */
    private Url url;
    /** 是否已暴露服务 */
    private volatile boolean isExport;

    ServiceConfig(T ref, Class<T> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
    }

    //---------------------------------------------------------------------------------------------------------

    @Override
    void check() {
        Preconditions.checkNotNull(this.ref, "provider object must be not null");
        Preconditions.checkNotNull(this.interfaceClass, "provider object interface must be not");

        //检查ref和interfaceClass的合法性
        if (!this.interfaceClass.isInterface()) {
            throw new IllegalStateException("the class '" + this.interfaceClass.getName() + "' is not a interface");
        }

        Class<?> serviceClass = this.ref.getClass();
        if (!this.interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalStateException("service class " + serviceClass.getName() + " must implements the certain interface '" + this.interfaceClass.getName() + "'");
        }

        Preconditions.checkArgument(this.rate > 0, "rate must be greater than 0");

        this.applicationConfig.check();
        this.serverConfig.check();
        if (this.registryConfig != null) {
            this.registryConfig.check();
        }
    }

    /**
     * 暴露服务
     */
    public void export() {
        if (!isExport) {
            check();

            Map<String, String> params = new HashMap<>(50);
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.SERIALIZE_KEY, serialize + "");
            params.put(Constants.BYTE_CODE_INVOKE_KEY, Boolean.toString(InvokeType.JAVASSIST.equals(invokeType)));
            params.put(Constants.VERSION_KEY, version);
            params.put(Constants.COMPRESSION_KEY, Integer.toString(compressionType.getId()));
            params.put(Constants.PARALLELISM_KEY, parallelism + "");
            params.put(Constants.RATE_KEY, rate + "");
            params.put(Constants.INTERFACE_KEY, interfaceClass.getName());

            url = createURL(
                    applicationConfig,
                    NetUtils.getIpPort(serverConfig.getHost(), serverConfig.getPort()),
                    registryConfig,
                    params,
                    protocolType);
            Preconditions.checkNotNull(url);

            Clusters.export(url, interfaceClass, ref);

            isExport = true;
        }

    }

    /**
     * 暴露服务
     */
    public void exportSync() throws Exception {
        export();

        synchronized (this) {
            wait();
        }
    }

    /**
     * 取消服务注册
     */
    public void disable() {
        if (isExport) {
            Clusters.disableService(url);
        }
    }

    //---------------------------------------builder------------------------------------------------------------

    public ServiceConfig<T> appName(String appName) {
        if (!isExport) {
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ServiceConfig<T> bind(int port) {
        if (!isExport) {
            this.serverConfig = new ServerConfig(port);
        }
        return this;
    }

    public ServiceConfig<T> bind(String host, int port) {
        if (!isExport) {
            this.serverConfig = new ServerConfig(host, port);
        }
        return this;
    }

    public ServiceConfig<T> urls(String... urls) {
        if (!isExport) {
            this.registryConfig = new DirectURLsRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ServiceConfig<T> zookeeper(String address) {
        if (!isExport) {
            this.registryConfig = new ZookeeperRegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig<T> redis(String address) {
        if (!isExport) {
            this.registryConfig = new RedisRegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig<T> registrySessionTimeout(long sessionTimeout) {
        if (!isExport) {
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ServiceConfig<T> registryWatchInterval(long watchInterval) {
        if (!isExport) {
            this.registryConfig.setWatchInterval(watchInterval);
        }
        return this;
    }

    public ServiceConfig<T> serviceName(String serviceName) {
        if (!isExport) {
            this.serviceName = serviceName;
        }
        return this;
    }

    public ServiceConfig<T> serialize(Class<? extends Serializer> serializerClass) {
        if (!isExport) {
            this.serialize = Serializers.getOrLoadSerializer(serializerClass);
        }
        return this;
    }

    public ServiceConfig<T> serialize(SerializerType serializerType) {
        if (!isExport) {
            this.serialize = serializerType.getCode();
        }
        return this;
    }

    public ServiceConfig<T> javaInvoke() {
        this.invokeType = InvokeType.JAVA;
        return this;
    }

    public ServiceConfig<T> javassistInvoke() {
        this.invokeType = InvokeType.JAVASSIST;
        return this;
    }

    public ServiceConfig<T> version(String version) {
        this.version = version;
        return this;
    }

    public ServiceConfig<T> compress(CompressionType compressionType) {
        this.compressionType = compressionType;
        return this;
    }

    public ServiceConfig<T> actorLike() {
        this.parallelism = false;
        return this;
    }

    public ServiceConfig<T> rate(int rate) {
        this.rate = rate;
        return this;
    }

    public ServiceConfig<T> protocol(ProtocolType protocolType) {
        this.protocolType = protocolType;
        return this;
    }

    //setter && getter

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public AbstractRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Object getRef() {
        return ref;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getSerialize() {
        return serialize;
    }

    public InvokeType getInvokeType() {
        return invokeType;
    }

    public String getVersion() {
        return version;
    }

    public CompressionType isCompression() {
        return compressionType;
    }

    public boolean isParallelism() {
        return parallelism;
    }

    public int getRate() {
        return rate;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }
}
