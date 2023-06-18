package org.kin.kinrpc.rpc.common.config;

import org.kin.kinrpc.rpc.common.constants.ReferenceConstants;

import javax.annotation.Nullable;

/**
 * 服务引用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ReferenceConfig<T> extends AbstractInterfaceConfig<T, ReferenceConfig<T>> {
    /** 集群处理, 默认是failover */
    // TODO: 待实现
    private String cluster;
    /** 负载均衡类型, 默认round robin */
    private String loadBalance = LoadBalanceType.ROUND_ROBIN.getName();
    /** 路由类型, 默认none */
    private String router = RouterType.NONE.getName();
    /** async rpc call 事件通知 */
    // TODO: 待实现
//    private final List<Notifier<?>> notifiers = new ArrayList<>();
    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
    // TODO: 待实现
    private boolean generic;
    /** 服务connection ssl配置 */
    private SslConfig ssl;


    //----------------------------------------------------------------方法级配置, 如果方法没有特殊配置, 则取这个
    /**
     * rpc call timeout
     * todo 对于service端, 如果调用超时, 那么仅仅会打印log
     */
    private int callTimeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
    /** 是否服务调用粘黏 */
    private boolean sticky;


    /** 服务引用, 也就是客户端服务invoker */
    private volatile T reference;

    public static <T> ReferenceConfig<T> create(Class<T> interfaceClass) {
        return new ReferenceConfig<T>().interfaceClass(interfaceClass);
    }

    private ReferenceConfig() {
    }

    /**
     * 创建服务引用
     *
     * @return 服务引用
     */
    public T refer() {
        // TODO: 2023/6/16
    }

    //setter && getter
    public String getCluster() {
        return cluster;
    }

    public ReferenceConfig<T> cluster(String cluster) {
        this.cluster = cluster;
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

    public int getCallTimeout() {
        return callTimeout;
    }

    public ReferenceConfig<T> callTimeout(int callTimeout) {
        this.callTimeout = callTimeout;
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

    @Nullable
    public T getReference() {
        return reference;
    }
}
