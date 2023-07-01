package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.bootstrap.ReferenceBootstrap;
import org.kin.kinrpc.constants.ReferenceConstants;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 服务引用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ReferenceConfig<T> extends AbstractInterfaceConfig<T, ReferenceConfig<T>> {
    /** 服务方法配置 */
    private final List<MethodConfig> methods = new ArrayList<>();
    /** 集群处理, 默认是failover */
    private String cluster = ClusterType.FAIL_FAST.getName();
    /** 负载均衡类型, 默认round robin */
    private String loadBalance = LoadBalanceType.ROUND_ROBIN.getName();
    /** 路由类型, 默认none */
    private String router = RouterType.NONE.getName();
    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
    private boolean generic;
    /** 服务connection ssl配置 */
    private SslConfig ssl;
    /** bootstrap 类型 */
    private String bootstrap = "kinrpc";

    //----------------------------------------------------------------方法级配置, 如果方法没有特殊配置, 则取这个
    /**
     * rpc call timeout(ms)
     */
    private int rpcTimeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
    /** 是否服务调用粘黏 */
    private boolean sticky;

    //----------------------------------------------------------------动态变量, lazy init
    private transient ReferenceBootstrap<T> referenceBootstrap;

    public static <T> ReferenceConfig<T> create(Class<T> interfaceClass) {
        return new ReferenceConfig<T>().interfaceClass(interfaceClass);
    }

    private ReferenceConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        Set<String> availableMethodNames = new HashSet<>();
        for (Method method : getInterfaceClass().getDeclaredMethods()) {
            boolean notExists = availableMethodNames.add(method.getName());
            if (!isGeneric() && !notExists) {
                throw new IllegalConfigException(String.format("service interface method name '%s' conflict, does not support method overload now", method.getName()));
            }
        }
        for (MethodConfig methodConfig : methods) {
            methodConfig.checkValid();

            //检查方法名是否合法
            String name = methodConfig.getName();
            if (!availableMethodNames.contains(name)) {
                throw new IllegalConfigException(String.format("method '%s' is not found in interface '%s'", name, getInterfaceClass().getName()));
            }
        }
        if (generic && !GenericService.class.equals(getInterfaceClass())) {
            //开启泛化, 服务接口必须为GenericService
            throw new IllegalConfigException(String.format("open generic, interface class must be org.kin.kinrpc.GenericService, but actually is %s", getInterfaceClass().getName()));
        }

        check(StringUtils.isNotBlank(cluster), "cluster must be not blank");
        check(StringUtils.isNotBlank(loadBalance), "loadBalance must be not blank");
        check(StringUtils.isNotBlank(router), "router must be not blank");
        check(StringUtils.isNotBlank(bootstrap), "bootstrap must be not blank");

        check(rpcTimeout > 0, "global method rpc call timeout must be greater than 0");
        check(retries > 0, "global method rpc call retry times must be greater than 0");
    }

    /**
     * 缺省配置, 设置默认值
     */
    @Override
    protected void setUpDefaultConfig() {
        super.setUpDefaultConfig();
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    @SuppressWarnings("unchecked")
    public synchronized T refer() {
        setUpDefaultConfig();
        checkValid();

        if (Objects.isNull(referenceBootstrap)) {
            referenceBootstrap = ExtensionLoader.getExtension(ReferenceBootstrap.class, bootstrap, this);
        }

        return referenceBootstrap.refer();
    }

    /**
     * 取消服务引用
     */
    public synchronized void unRefer() {
        if (Objects.isNull(referenceBootstrap)) {
            return;
        }

        referenceBootstrap.unRefer();
    }

    //setter && getter

    public ReferenceConfig<T> jvm() {
        return bootstrap(BootstrapType.JVM);
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    public ReferenceConfig<T> method(MethodConfig method) {
        this.methods.add(method);
        return this;
    }

    public ReferenceConfig<T> methods(MethodConfig... methods) {
        return methods(Arrays.asList(methods));
    }

    public ReferenceConfig<T> methods(Collection<MethodConfig> methods) {
        this.methods.addAll(methods);
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public ReferenceConfig<T> cluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public ReferenceConfig<T> cluster(ClusterType clusterType) {
        this.cluster = clusterType.getName();
        return this;
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public ReferenceConfig<T> loadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
        return this;
    }

    public ReferenceConfig<T> loadBalance(LoadBalanceType loadBalanceType) {
        return loadBalance(loadBalanceType.getName());
    }

    public String getRouter() {
        return router;
    }

    public ReferenceConfig<T> router(String router) {
        this.router = router;
        return this;
    }

    public ReferenceConfig<T> router(RouterType routerType) {
        return router(routerType.getName());
    }

    public boolean isGeneric() {
        return generic;
    }

    public ReferenceConfig<T> generic() {
        this.generic = true;
        return this;
    }

    public int getRpcTimeout() {
        return rpcTimeout;
    }

    public ReferenceConfig<T> rpcTimeout(int rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public ReferenceConfig<T> retries(int retries) {
        this.retries = retries;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public ReferenceConfig<T> async() {
        this.async = true;
        return this;
    }

    public boolean isSticky() {
        return sticky;
    }

    public ReferenceConfig<T> sticky() {
        this.sticky = true;
        return this;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public ReferenceConfig<T> ssl(SslConfig ssl) {
        this.ssl = ssl;
        return this;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public ReferenceConfig<T> bootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public ReferenceConfig<T> bootstrap(BootstrapType bootstrapType) {
        this.bootstrap = bootstrapType.getName();
        return this;
    }
}
