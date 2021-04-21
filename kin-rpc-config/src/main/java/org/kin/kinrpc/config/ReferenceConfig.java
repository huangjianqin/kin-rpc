package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.cluster.Router;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceConfig<T> extends AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(ReferenceConfig.class);

    /** 应用配置 */
    private ApplicationConfig applicationConfig = new ApplicationConfig();
    /** 注册中心配置 */
    private AbstractRegistryConfig registryConfig;
    /** 服务接口 */
    private Class<T> interfaceClass;
    /** 服务名 */
    private String serviceName;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 重试次数 */
    private int retryTimes;
    /** 重试等待时间(即两次重试间隔时间)(ms) */
    private long retryInterval = Constants.RETRY_INTERVAL;
    /** 负载均衡类型 */
    private String loadBalanceType = LoadBalanceType.ROUNDROBIN.getType();
    /** 路由类型 */
    private String routerType = RouterType.NONE.getType();
    /** 客户端服务invoker调用类型 */
    private ProxyType proxyType = ProxyType.JAVASSIST;
    /** 服务限流, 每秒发送多少个 */
    private int tps = Constants.REFERENCE_DEFAULT_TPS;
    /** 是否支持异步rpc call */
    private boolean async;
    /** async rpc call 事件通知 */
    private List<Notifier<?>> notifiers = new ArrayList<>();
    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
    private boolean useGeneric;
    /** rpc call超时 */
    private long callTimeout = Constants.RPC_CALL_TIMEOUT;
    /** 是否允许ssl */
    private boolean ssl;
    /** 额外参数, 主要用于支持不同协议层的额外配置 */
    private Map<String, Object> attachment = new HashMap<>();

    /** 唯一url */
    private Url url;
    /** 服务引用, 也就是客户端服务invoker */
    private volatile T reference;
    /** 是否已暴露引用 */
    private boolean isReference;

    ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();

        //默认netty channel options
        Map<String, Object> nettyOptions = new HashMap<>(4);
        nettyOptions.put(Constants.NETTY_NODELAY_KEY, true);
        nettyOptions.put(Constants.NETTY_CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
        //receive窗口缓存8mb
        nettyOptions.put(Constants.NETTY_RCVBUF_KEY, 8 * 1024 * 1024);
        //send窗口缓存64kb
        nettyOptions.put(Constants.NETTY_SNDBUF_KEY, 64 * 1024);
        attach(nettyOptions);
    }

    //---------------------------------------------------------------------------------------------------------

    @Override
    void check() {
        Preconditions.checkNotNull(this.applicationConfig, "reference must need to configure application");
        this.applicationConfig.check();
        Preconditions.checkNotNull(this.registryConfig, "reference must need to configure register");
        this.registryConfig.check();
        Preconditions.checkNotNull(this.interfaceClass, "reference subscribed interface must be not null");
        Preconditions.checkArgument(this.retryInterval > 0, "retryTimeout must greater than 0");
        Preconditions.checkArgument(this.tps > 0, "tps must be greater than 0");
        Preconditions.checkArgument(this.callTimeout > 0, "callTimeout must greater than 0");
    }

    public synchronized T get() {
        if (!isReference) {
            check();

            Map<String, String> params = new HashMap<>(50);
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.VERSION_KEY, version);
            params.put(Constants.RETRY_TIMES_KEY, retryTimes + "");
            params.put(Constants.RETRY_INTERVAL_KEY, retryInterval + "");
            params.put(Constants.LOADBALANCE_KEY, loadBalanceType);
            params.put(Constants.ROUTER_KEY, routerType);
            params.put(Constants.BYTE_CODE_INVOKE_KEY, Boolean.toString(ProxyType.JAVASSIST.equals(proxyType)));
            params.put(Constants.TPS_KEY, tps + "");
            params.put(Constants.ASYNC_KEY, Boolean.toString(async));
            params.put(Constants.GENERIC_KEY, Boolean.toString(useGeneric));
            params.put(Constants.CALL_TIMEOUT_KEY, callTimeout + "");
            params.put(Constants.INTERFACE_KEY, interfaceClass.getName());
            params.put(Constants.SSL_ENABLED_KEY, Boolean.toString(ssl));

            url = createURL(
                    applicationConfig,
                    NetUtils.getIp(),
                    registryConfig,
                    params,
                    null);
            Preconditions.checkNotNull(url);

            //关联
            url.attach(attachment);

            reference = Clusters.reference(url, interfaceClass, notifiers);

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
                ExceptionUtils.throwExt(e);
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

    public ReferenceConfig<T> serviceName(String serviceName) {
        if (!isReference) {
            this.serviceName = serviceName;
        }
        return this;
    }

    public ReferenceConfig<T> version(String version) {
        if (!isReference) {
            this.version = version;
        }
        return this;
    }

    public ReferenceConfig<T> urls(String... urls) {
        if (!isReference) {
            this.registryConfig = new DirectURLsRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ReferenceConfig<T> jvm() {
        if (!isReference) {
            this.registryConfig = new DirectURLsRegistryConfig("jvm://0.0.0.0");
        }
        return this;
    }

    public ReferenceConfig<T> registry(AbstractRegistryConfig registryConfig) {
        if (!isReference) {
            this.registryConfig = registryConfig;
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
            this.retryInterval = retryTimeout;
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

    public ReferenceConfig<T> loadbalance(Class<? extends LoadBalance> loadBalanceClass) {
        if (!isReference) {
            this.loadBalanceType = loadBalanceClass.getName();
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

    public ReferenceConfig<T> router(Class<? extends Router> routerClass) {
        if (!isReference) {
            this.routerType = routerClass.getName();
        }
        return this;
    }

    public ReferenceConfig<T> javaProxy() {
        if (!isReference) {
            this.proxyType = ProxyType.JAVA;
        }
        return this;
    }

    public ReferenceConfig<T> javassistProxy() {
        if (!isReference) {
            this.proxyType = ProxyType.JAVASSIST;
        }
        return this;
    }

    public ReferenceConfig<T> tps(int tps) {
        if (!isReference) {
            this.tps = tps;
        }
        return this;
    }

    public ReferenceConfig<T> async() {
        if (!isReference) {
            this.async = true;
        }
        return this;
    }

    public ReferenceConfig<T> notify(Class<? extends Notifier<?>>... notifierClasses) {
        if (!isReference) {
            for (Class<? extends Notifier<?>> notifierClass : notifierClasses) {
                notify(ClassUtils.instance(notifierClass));
            }
        }
        return this;
    }

    public ReferenceConfig<T> notify(Notifier<?>... notifiers) {
        if (!isReference) {
            this.notifiers.addAll(Arrays.asList(notifiers));
        }
        return this;
    }

    public ReferenceConfig<T> useGeneric() {
        if (!isReference) {
            this.useGeneric = true;
        }
        return this;
    }

    public ReferenceConfig<T> callTimeout(long callTimeout) {
        if (!isReference) {
            this.callTimeout = callTimeout;
        }
        return this;
    }

    public ReferenceConfig<T> enableSsl() {
        if (!isReference) {
            this.ssl = true;
        }
        return this;
    }

    public ReferenceConfig<T> attach(String key, Object value) {
        if (!isReference) {
            this.attachment.put(key, value);
        }
        return this;
    }

    public ReferenceConfig<T> attach(Map<String, Object> attachment) {
        if (!isReference) {
            this.attachment.putAll(attachment);
        }
        return this;
    }

    //getter
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public AbstractRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public String getLoadBalanceType() {
        return loadBalanceType;
    }

    public String getRouterType() {
        return routerType;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public int getTps() {
        return tps;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isUseGeneric() {
        return useGeneric;
    }

    public boolean isSsl() {
        return ssl;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }
}
