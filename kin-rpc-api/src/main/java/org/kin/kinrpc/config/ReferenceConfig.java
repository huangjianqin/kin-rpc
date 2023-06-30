package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.bootstrap.ReferenceBootstrap;
import org.kin.kinrpc.constants.ReferenceConstants;

import java.util.*;

/**
 * 服务引用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ReferenceConfig<T> extends AbstractInterfaceConfig<T, ReferenceConfig<T>> {
    // TODO: 2023/6/29 记录服务引用次数, 多次重复引用打warn日志, 因为这个操作会很浪费资源
    // TODO: 2023/6/25 校验方法名是否一致
    /** 服务方法配置 */
    private final List<MethodConfig> methods = new ArrayList<>();
    /** 集群处理, 默认是failover */
    private String cluster = ClusterType.FAIL_FAST.getName();
    /** 负载均衡类型, 默认round robin */
    private String loadBalance = LoadBalanceType.ROUND_ROBIN.getName();
    /** 路由类型, 默认none */
    private String router = RouterType.NONE.getName();
    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
    // TODO: 待实现
    private boolean generic;
    /** 服务connection ssl配置 */
    private SslConfig ssl;
    /** bootstrap 类型 */
    private String bootstrap = "kinrpc";

    //----------------------------------------------------------------方法级配置, 如果方法没有特殊配置, 则取这个
    /**
     * rpc call timeout
     * todo 对于service端, 如果调用超时, 那么仅仅会打印log
     */
    private int rpcTimeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
    /** 是否服务调用粘黏 */
    private boolean sticky;

    private transient ReferenceBootstrap<T> referenceBootstrap;

    public static <T> ReferenceConfig<T> create(Class<T> interfaceClass) {
        return new ReferenceConfig<T>().interfaceClass(interfaceClass);
    }

    private ReferenceConfig() {
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    @SuppressWarnings("unchecked")
    public synchronized T refer() {
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
}
