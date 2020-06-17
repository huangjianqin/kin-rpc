package org.kin.kinrpc.config;


import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.serializer.SerializerType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServiceConfig extends AbstractConfig {
    /** 配置 */
    private ApplicationConfig applicationConfig = new ApplicationConfig();
    private ServerConfig serverConfig = ServerConfig.DEFAULT;
    private AbstractRegistryConfig registryConfig;
    private Object ref;
    private Class<?> interfaceClass;
    private String serviceName;
    private String serialize = SerializerType.KRYO.getType();
    private InvokeType invokeType = InvokeType.JAVASSIST;
    private String version = "0.1.0.0";
    private boolean compression;
    /** 默认支持并发执行 */
    private boolean parallelism = true;
    /** 流控, 每秒最多处理多少个request */
    private int rate = Constants.PROVIDER_REQUEST_THRESHOLD;

    private Url url;
    private volatile boolean isExport;

    ServiceConfig(Object ref, Class<?> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
    }

    //---------------------------------------------------------------------------------------------------------

    @Override
    void check() throws Exception{
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
    public void export() throws Exception {
        if (!isExport) {
            check();

            Map<String, String> params = new HashMap<>(50);
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.SERIALIZE_KEY, serialize);
            params.put(Constants.BYTE_CODE_INVOKE_KEY, Boolean.toString(InvokeType.JAVASSIST.equals(invokeType)));
            params.put(Constants.VERSION_KEY, version);
            params.put(Constants.COMPRESSION_KEY, Boolean.toString(compression));
            params.put(Constants.PARALLELISM_KEY, parallelism + "");
            params.put(Constants.RATE_KEY, rate + "");

            url = createURL(applicationConfig, NetUtils.getIpPort(serverConfig.getHost(), serverConfig.getPort()), registryConfig, params);
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

        synchronized (this){
            wait();
        }
    }

    /**
     * 取消服务注册
     */
    public void disable() {
        if (isExport) {
            Clusters.disableService(url, interfaceClass);
        }
    }

    //---------------------------------------builder------------------------------------------------------------

    public ServiceConfig appName(String appName) {
        if (!isExport) {
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ServiceConfig bind(int port) {
        if (!isExport) {
            this.serverConfig = new ServerConfig(port);
        }
        return this;
    }

    public ServiceConfig bind(String host, int port) {
        if (!isExport) {
            this.serverConfig = new ServerConfig(host, port);
        }
        return this;
    }

    public ServiceConfig urls(String... urls) {
        if (!isExport) {
            this.registryConfig = new DirectURLsRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ServiceConfig zookeeper(String address) {
        if (!isExport) {
            this.registryConfig = new ZookeeperRegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig zookeeper2(String address) {
        if (!isExport) {
            this.registryConfig = new Zookeeper2RegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig registrySessionTimeout(int sessionTimeout) {
        if (!isExport) {
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ServiceConfig serviceName(String serviceName) {
        if (!isExport) {
            this.serviceName = serviceName;
        }
        return this;
    }

    public ServiceConfig serialize(String serialize) {
        if (!isExport) {
            this.serialize = serialize;
        }
        return this;
    }

    public ServiceConfig javaInvoke() {
        this.invokeType = InvokeType.JAVA;
        return this;
    }

    public ServiceConfig javassistInvoke() {
        this.invokeType = InvokeType.JAVASSIST;
        return this;
    }

    public ServiceConfig version(String version) {
        this.version = version;
        return this;
    }

    public ServiceConfig compress() {
        this.compression = true;
        return this;
    }

    public ServiceConfig uncompress() {
        this.compression = false;
        return this;
    }

    public ServiceConfig actorLike() {
        this.parallelism = false;
        return this;
    }

    public ServiceConfig rate(int rate) {
        this.rate = rate;
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

    public String getSerialize() {
        return serialize;
    }

    public InvokeType getInvokeType() {
        return invokeType;
    }

    public String getVersion() {
        return version;
    }

    public boolean isCompression() {
        return compression;
    }

    public boolean isParallelism() {
        return parallelism;
    }

    public int getRate() {
        return rate;
    }
}
