package org.kin.kinrpc.config;


import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.serialization.SerializationType;
import org.kin.transport.netty.CompressionType;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

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
    /** 服务所属组 */
    private String group = "";
    /** 服务名 */
    private String service;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 序列化类型, 在kinrpc协议下生效 */
    private int serializationCode = SerializationType.KRYO.getCode();
    /** 服务调用类型(invoker收到请求后, 用哪种方式调用服务) */
    private ProxyType proxyType = ProxyType.JAVASSIST;

    /** 压缩类型, 在kinrpc协议下生效 todo 考虑降低耦合性 */
    private CompressionType compressionType = CompressionType.NONE;
    /** 默认支持并发执行 */
    private boolean parallelism = true;
    /** 流控, 每秒最多处理多少个request */
    private int tps = Constants.PROVIDER_DEFAULT_TPS;
    /** 协议类型 */
    private ProtocolType protocolType = ProtocolType.KINRPC;
    /** 是否允许ssl */
    private boolean ssl;
    /** 额外参数, 主要用于支持不同协议层的额外配置 */
    private Map<String, Object> attachment = new HashMap<>();

    /** 唯一url */
    private Url url;
    /** 是否已暴露服务 */
    private volatile boolean isExport;

    ServiceConfig(T ref, Class<T> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
        this.service = interfaceClass.getCanonicalName();

        //默认netty channel options
        Map<String, Object> nettyOptions = new HashMap<>(5);
        nettyOptions.put(Constants.NETTY_NODELAY_KEY, true);
        nettyOptions.put(Constants.NETTY_KEEPALIVE_KEY, true);
        nettyOptions.put(Constants.NETTY_REUSEADDR_KEY, true);
        //receive窗口缓存8mb
        nettyOptions.put(Constants.NETTY_RCVBUF_KEY, 8 * 1024 * 1024);
        //send窗口缓存64kb
        nettyOptions.put(Constants.NETTY_SNDBUF_KEY, 64 * 1024);
        attach(nettyOptions);
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

        //检查暴露的服务接口是否于实现实例满足继承关系
        Class<?> serviceClass = this.ref.getClass();
        if (!this.interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalStateException("service class " + serviceClass.getName() + " must implements the certain interface '" + this.interfaceClass.getName() + "'");
        }

        //kinrpc协议下, 检查暴露的服务接口的参数和返回值是否满足条件
        //其余兼容协议不做检查, 像grpc或者rsocket这种, 返回值和参数就是没有实现Serializable
        if (ProtocolType.KINRPC.equals(protocolType)) {
            checkInterfaceClass();
        }

        Preconditions.checkArgument(this.tps > 0, "tps must be greater than 0");

        this.applicationConfig.check();
        this.serverConfig.check();
        if (this.registryConfig != null) {
            this.registryConfig.check();
        }
    }

    /**
     * 检查暴露的服务接口的参数和返回值是否满足条件
     */
    private void checkInterfaceClass() {
        for (Method method : interfaceClass.getMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (!parameterType.isPrimitive() && !Serializable.class.isAssignableFrom(parameterType)) {
                    //如果参数不是基础类型, 且没有实现Serializable
                    throw new IllegalArgumentException(String.format("arg '' in method '%s' doesn't implement Serializable", method));
                }
            }

            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive() &&
                    !Void.class.equals(returnType) &&
                    !Future.class.isAssignableFrom(returnType) &&
                    !Serializable.class.isAssignableFrom(returnType)) {
                //如果返回值不是Future, 且没有实现Serializable
                throw new IllegalArgumentException(String.format("method '%s' return type doesn't implement Serializable", method));
            }
        }
    }

    /**
     * 暴露服务
     */
    public void export() {
        if (!isExport) {
            check();

            Map<String, String> params = new HashMap<>(50);
            params.put(Constants.SERVICE_KEY, service);
            params.put(Constants.GROUP_KEY, group);
            params.put(Constants.VERSION_KEY, version);
            params.put(Constants.SERIALIZATION_KEY, serializationCode + "");
            params.put(Constants.BYTE_CODE_INVOKE_KEY, Boolean.toString(ProxyType.JAVASSIST.equals(proxyType)));
            params.put(Constants.COMPRESSION_KEY, Integer.toString(compressionType.getId()));
            params.put(Constants.PARALLELISM_KEY, parallelism + "");
            params.put(Constants.TPS_KEY, tps + "");
            params.put(Constants.INTERFACE_KEY, interfaceClass.getName());
            params.put(Constants.SSL_ENABLED_KEY, Boolean.toString(ssl));

            url = createURL(
                    applicationConfig,
                    NetUtils.getIpPort(serverConfig.getHost(), serverConfig.getPort()),
                    registryConfig,
                    params,
                    protocolType);
            Preconditions.checkNotNull(url);
            //关联
            url.attach(attachment);

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

    public ServiceConfig<T> app(String app) {
        if (!isExport) {
            this.applicationConfig = new ApplicationConfig(app);
        }
        return this;
    }

    public ServiceConfig<T> group(String group) {
        if (!isExport) {
            this.group = group;
        }
        return this;
    }

    public ServiceConfig<T> service(String service) {
        if (!isExport) {
            this.service = service;
        }
        return this;
    }

    public ServiceConfig<T> version(String version) {
        this.version = version;
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

    public ServiceConfig<T> registry(AbstractRegistryConfig registryConfig) {
        if (registryConfig instanceof DirectURLsRegistryConfig) {
            throw new IllegalArgumentException("kinrpc service does not support direct url registry");
        }
        if (!isExport) {
            this.registryConfig = registryConfig;
        }
        return this;
    }

    public ServiceConfig<T> serialization(SerializationType serializationType) {
        if (!isExport) {
            this.serializationCode = serializationType.getCode();
        }
        return this;
    }

    public ServiceConfig<T> serialization(int serializationCode) {
        if (!isExport) {
            this.serializationCode = serializationCode;
        }
        return this;
    }

    public ServiceConfig<T> javaProxy() {
        if (!isExport) {
            this.proxyType = ProxyType.JAVA;
        }
        return this;
    }

    public ServiceConfig<T> javassistProxy() {
        if (!isExport) {
            this.proxyType = ProxyType.JAVASSIST;
        }
        return this;
    }

    public ServiceConfig<T> compress(CompressionType compressionType) {
        if (!isExport) {
            this.compressionType = compressionType;
        }
        return this;
    }

    public ServiceConfig<T> actorLike() {
        if (!isExport) {
            this.parallelism = false;
        }
        return this;
    }

    public ServiceConfig<T> tps(int tps) {
        if (!isExport) {
            this.tps = tps;
        }
        return this;
    }

    public ServiceConfig<T> protocol(ProtocolType protocolType) {
        if (!isExport) {
            this.protocolType = protocolType;
        }
        return this;
    }

    public ServiceConfig<T> enableSsl() {
        if (!isExport) {
            this.ssl = true;
        }
        return this;
    }

    public ServiceConfig<T> attach(String key, Object value) {
        if (!isExport) {
            this.attachment.put(key, value);
        }
        return this;
    }

    public ServiceConfig<T> attach(Map<String, Object> attachment) {
        if (!isExport) {
            this.attachment.putAll(attachment);
        }
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

    public String getService() {
        return service;
    }

    public int getSerializationCode() {
        return serializationCode;
    }

    public ProxyType getProxyType() {
        return proxyType;
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

    public int getTps() {
        return tps;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public boolean isSsl() {
        return ssl;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }
}
