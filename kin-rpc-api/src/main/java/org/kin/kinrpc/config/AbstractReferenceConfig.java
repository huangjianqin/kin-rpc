package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/7
 */
public abstract class AbstractReferenceConfig<ARC extends AbstractReferenceConfig<ARC>>
        extends AbstractInterfaceConfig<ARC> {
    /** 集群处理, 默认是failover */
    private String cluster;
    /** 负载均衡类型, 默认round robin */
    private String loadBalance;
    /** 路由类型, 默认none */
    private String router;
    /** 是否是泛化接口引用 */
    private Boolean generic;
    /** 服务connection ssl配置 */
    private SslConfig ssl;
    /** bootstrap 类型 */
    private String bootstrap;
    /** 指定服务由那些应用提供, 如果存在多个, 则逗号分隔 */
    private String provideBy;

    //----------------------------------------------------------------方法级配置, 如果方法没有特殊配置, 则取这个
    /** rpc call timeout(ms) */
    private Integer rpcTimeout;
    /** 失败后重试次数 */
    private Integer retries;
    /** 是否异步调用 */
    private Boolean async;
    /** 是否服务调用粘黏 */
    private Boolean sticky;

    @Override
    public void checkValid() {
        super.checkValid();
        check(getRegistries().size() > 0, "registry config must be config at least one");

        check(StringUtils.isNotBlank(cluster), "cluster must be not blank");
        check(StringUtils.isNotBlank(loadBalance), "loadBalance must be not blank");
        check(StringUtils.isNotBlank(router), "router must be not blank");
        check(StringUtils.isNotBlank(bootstrap), "reference bootstrap must be not blank");

        check(rpcTimeout > 0, "global method rpc call timeout must be greater than 0");
    }

    @Override
    protected boolean isJvmBootstrap() {
        return BootstrapType.JVM.getName().equalsIgnoreCase(bootstrap);
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.isNull(cluster)) {
            cluster = DefaultConfig.DEFAULT_REFERENCE_CLUSTER;
        }

        if (Objects.isNull(loadBalance)) {
            loadBalance = DefaultConfig.DEFAULT_REFERENCE_LOADBALANCE;
        }

        if (Objects.isNull(router)) {
            router = DefaultConfig.DEFAULT_REFERENCE_ROUTER;
        }

        if (Objects.isNull(generic)) {
            generic = DefaultConfig.DEFAULT_REFERENCE_GENERIC;
        }

        if (Objects.isNull(bootstrap)) {
            bootstrap = DefaultConfig.DEFAULT_REFERENCE_BOOTSTRAP;
        }


        if (Objects.isNull(rpcTimeout)) {
            rpcTimeout = DefaultConfig.DEFAULT_METHOD_TIMEOUT;
        }

        if (Objects.isNull(retries)) {
            retries = DefaultConfig.DEFAULT_METHOD_RETRIES;
        }

        if (Objects.isNull(async)) {
            async = DefaultConfig.DEFAULT_METHOD_ASYNC;
        }

        if (Objects.isNull(sticky)) {
            sticky = DefaultConfig.DEFAULT_METHOD_STICKY;
        }
    }

    //setter && getter
    public ARC jvm() {
        return bootstrap(BootstrapType.JVM);
    }

    public String getCluster() {
        return cluster;
    }

    public ARC cluster(String cluster) {
        this.cluster = cluster;
        return castThis();
    }

    public ARC cluster(ClusterType clusterType) {
        this.cluster = clusterType.getName();
        return castThis();
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public ARC loadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
        return castThis();
    }

    public ARC loadBalance(LoadBalanceType loadBalanceType) {
        return loadBalance(loadBalanceType.getName());
    }

    public String getRouter() {
        return router;
    }

    public ARC router(String router) {
        this.router = router;
        return castThis();
    }

    public ARC router(RouterType routerType) {
        return router(routerType.getName());
    }

    public Boolean isGeneric() {
        return Objects.nonNull(generic) ? generic : false;
    }

    public ARC generic() {
        return generic(true);
    }

    public ARC generic(boolean generic) {
        this.generic = generic;
        return castThis();
    }

    public Integer getRpcTimeout() {
        return rpcTimeout;
    }

    public ARC rpcTimeout(int rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
        return castThis();
    }

    public Integer getRetries() {
        return retries;
    }

    public ARC retries(int retries) {
        this.retries = retries;
        return castThis();
    }

    public Boolean isAsync() {
        return Objects.nonNull(async) ? async : false;
    }

    public ARC async() {
        return async(true);
    }

    public ARC async(boolean async) {
        this.async = async;
        return castThis();
    }

    public Boolean isSticky() {
        return Objects.nonNull(sticky) ? sticky : false;
    }

    public ARC sticky() {
        return sticky(true);
    }

    public ARC sticky(boolean sticky) {
        this.sticky = sticky;
        return castThis();
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public ARC ssl(SslConfig ssl) {
        this.ssl = ssl;
        return castThis();
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public ARC bootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return castThis();
    }

    public ARC bootstrap(BootstrapType bootstrapType) {
        this.bootstrap = bootstrapType.getName();
        return castThis();
    }

    public AbstractReferenceConfig<ARC> provideBy(String provideBy) {
        this.provideBy = provideBy;
        return this;
    }

    //----------------------
    public AbstractReferenceConfig<ARC> setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public AbstractReferenceConfig<ARC> setLoadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
        return this;
    }

    public AbstractReferenceConfig<ARC> setRouter(String router) {
        this.router = router;
        return this;
    }

    public Boolean getGeneric() {
        return generic;
    }

    public AbstractReferenceConfig<ARC> setGeneric(Boolean generic) {
        this.generic = generic;
        return this;
    }

    public AbstractReferenceConfig<ARC> setSsl(SslConfig ssl) {
        this.ssl = ssl;
        return this;
    }

    public AbstractReferenceConfig<ARC> setBootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public AbstractReferenceConfig<ARC> setRpcTimeout(Integer rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
        return this;
    }

    public AbstractReferenceConfig<ARC> setRetries(Integer retries) {
        this.retries = retries;
        return this;
    }

    public Boolean getAsync() {
        return async;
    }

    public AbstractReferenceConfig<ARC> setAsync(Boolean async) {
        this.async = async;
        return this;
    }

    public Boolean getSticky() {
        return sticky;
    }

    public AbstractReferenceConfig<ARC> setSticky(Boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    public String getProvideBy() {
        return provideBy;
    }

    public void setProvideBy(String provideBy) {
        this.provideBy = provideBy;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", cluster='" + cluster + '\'' +
                ", loadBalance='" + loadBalance + '\'' +
                ", router='" + router + '\'' +
                ", generic=" + generic +
                ", ssl=" + ssl +
                ", bootstrap='" + bootstrap + '\'' +
                ", provideBy='" + provideBy + '\'' +
                ", rpcTimeout=" + rpcTimeout +
                ", retries=" + retries +
                ", async=" + async +
                ", sticky=" + sticky;
    }
}
