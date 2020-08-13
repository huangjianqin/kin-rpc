package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceConfig<T> extends AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(ReferenceConfig.class);

    private ApplicationConfig applicationConfig = new ApplicationConfig();
    private AbstractRegistryConfig registryConfig;
    private Class<T> interfaceClass;
    private int timeout = Constants.REFERENCE_DEFAULT_CONNECT_TIMEOUT;
    private String serviceName;
    private int retryTimes;
    private long retryTimeout = Constants.RETRY_TIMEOUT;
    private String serialize = SerializerType.KRYO.getType();
    private String loadBalanceType = LoadBalanceType.ROUNDROBIN.getType();
    private String routerType = RouterType.NONE.getType();
    private InvokeType invokeType = InvokeType.JAVASSIST;
    private String version = "0.1.0.0";
    private boolean compression;
    /** 服务限流, 每秒发送多少个 */
    private int rate = Constants.REFERENCE_REQUEST_THRESHOLD;

    private Url url;
    private volatile T reference;
    private boolean isReference;

    ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
    }

    //---------------------------------------------------------------------------------------------------------

    @Override
    void check() {
        Preconditions.checkNotNull(this.applicationConfig, "reference must need to configure application");
        this.applicationConfig.check();
        Preconditions.checkNotNull(this.registryConfig, "reference must need to configure register");
        this.registryConfig.check();
        Preconditions.checkNotNull(this.interfaceClass, "reference subscribed interface must be not null");
        Preconditions.checkArgument(this.timeout > 0, "connection's timeout must greater than 0");
        Preconditions.checkArgument(this.retryTimeout > 0, "retrytimeout must greater than 0");
        Preconditions.checkArgument(this.rate > 0, "rate must be greater than 0");
    }

    public synchronized T get() {
        if (!isReference) {
            check();

            Map<String, String> params = new HashMap<>(50);
            params.put(Constants.TIMEOUT_KEY, timeout + "");
            params.put(Constants.RETRY_TIMES_KEY, retryTimes + "");
            params.put(Constants.RETRY_TIMEOUT_KEY, retryTimeout + "");
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.SERIALIZE_KEY, serialize);
            params.put(Constants.LOADBALANCE_KEY, loadBalanceType);
            params.put(Constants.ROUTER_KEY, routerType);
            params.put(Constants.BYTE_CODE_INVOKE_KEY, Boolean.toString(InvokeType.JAVASSIST.equals(invokeType)));
            params.put(Constants.VERSION_KEY, version);
            params.put(Constants.COMPRESSION_KEY, Boolean.toString(compression));
            params.put(Constants.RATE_KEY, rate + "");

            url = createURL(applicationConfig, NetUtils.getIp(), registryConfig, params);
            Preconditions.checkNotNull(url);

            reference = Clusters.reference(url, interfaceClass);

            isReference = true;
        }

        return reference;
    }

    public void disable() {
        if (isReference) {
            reference = null;

            try {
                Clusters.disableReference(url);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    //---------------------------------------builder------------------------------------------------------------

    public ReferenceConfig<T> appName(String appName) {
        if (!isReference) {
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ReferenceConfig<T> urls(String... urls) {
        if (!isReference) {
            this.registryConfig = new DirectURLsRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ReferenceConfig<T> zookeeper(String address) {
        if (!isReference) {
            this.registryConfig = new ZookeeperRegistryConfig(address);
        }
        return this;
    }

    public ReferenceConfig<T> redis(String address) {
        if (!isReference) {
            this.registryConfig = new RedisRegistryConfig(address);
        }
        return this;
    }

    public ReferenceConfig<T> registrySessionTimeout(long sessionTimeout) {
        if (!isReference) {
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ReferenceConfig<T> registryWatchInterval(long watchInterval) {
        if (!isReference) {
            this.registryConfig.setWatchInterval(watchInterval);
        }
        return this;
    }

    public ReferenceConfig<T> timeout(int timeout) {
        if (!isReference) {
            this.timeout = timeout;
        }
        return this;
    }

    public ReferenceConfig<T> serviceName(String serviceName) {
        if (!isReference) {
            this.serviceName = serviceName;
        }
        return this;
    }

    public ReferenceConfig<T> retry(int retryTimes) {
        if (!isReference) {
            this.retryTimes = retryTimes;
        }
        return this;
    }

    public ReferenceConfig<T> retryTimeout(long retryTimeout) {
        if (!isReference) {
            this.retryTimeout = retryTimeout;
        }
        return this;
    }

    public ReferenceConfig<T> serialize(String serialize) {
        if (!isReference) {
            this.serialize = serialize;
        }
        return this;
    }

    public ReferenceConfig<T> serialize(SerializerType serializerType) {
        if (!isReference) {
            this.serialize = serializerType.getType();
        }
        return this;
    }

    public ReferenceConfig<T> loadbalance(String loadbalance) {
        if (!isReference) {
            this.loadBalanceType = loadbalance;
        }
        return this;
    }

    public ReferenceConfig<T> loadbalance(LoadBalanceType loadBalanceType) {
        if (!isReference) {
            this.loadBalanceType = loadBalanceType.getType();
        }
        return this;
    }

    public ReferenceConfig<T> router(String router) {
        if (!isReference) {
            this.routerType = router;
        }
        return this;
    }

    public ReferenceConfig<T> router(RouterType routerType) {
        if (!isReference) {
            this.routerType = routerType.getType();
        }
        return this;
    }

    public ReferenceConfig<T> javaInvoke() {
        this.invokeType = InvokeType.JAVA;
        return this;
    }

    public ReferenceConfig<T> javassistInvoke() {
        this.invokeType = InvokeType.JAVASSIST;
        return this;
    }

    public ReferenceConfig<T> version(String version) {
        this.version = version;
        return this;
    }

    public ReferenceConfig<T> compress() {
        this.compression = true;
        return this;
    }

    public ReferenceConfig<T> uncompress() {
        this.compression = false;
        return this;
    }

    public ReferenceConfig<T> rate(int rate) {
        this.rate = rate;
        return this;
    }

    //setter && getter

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public AbstractRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public long getRetryTimeout() {
        return retryTimeout;
    }

    public String getSerialize() {
        return serialize;
    }

    public String getLoadBalanceType() {
        return loadBalanceType;
    }

    public String getRouterType() {
        return routerType;
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

    public int getRate() {
        return rate;
    }
}
