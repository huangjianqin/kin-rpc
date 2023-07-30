package org.kin.kinrpc.config;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/7/7
 */
public final class DefaultConfig {

    //--------------------------------------------------------------------interface
    /** 默认组 */
    public static final String DEFAULT_GROUP = "default";
    /** 默认版本号 */
    public static final String DEFAULT_VERSION = "0.1.0.0";
    /** 默认序列化方式 */
    public static final String DEFAULT_SERIALIZATION = "jsonb";


    //--------------------------------------------------------------------method
    /** @see MethodConfig#getTimeout() */
    public static final Integer DEFAULT_METHOD_TIMEOUT = 3000;
    /** @see MethodConfig#getRetries() */
    public static final Integer DEFAULT_METHOD_RETRIES = 3;
    /** @see MethodConfig#isAsync() */
    public static final Boolean DEFAULT_METHOD_ASYNC = false;
    /** @see MethodConfig#isSticky() */
    public static final Boolean DEFAULT_METHOD_STICKY = false;
    /** @see MethodConfig#isValidation() */
    public static final Boolean DEFAULT_METHOD_VALIDATION = true;


    //--------------------------------------------------------------------executor
    /** @see ExecutorConfig#getCorePoolSize() */
    public static final Integer DEFAULT_EXECUTOR_CORE_POOL_SIZE = SysUtils.getSuitableThreadNum();
    /** @see ExecutorConfig#getMaxPoolSize() */
    public static final Integer DEFAULT_EXECUTOR_MAX_POOL_SIZE = Integer.MAX_VALUE;
    /** @see ExecutorConfig#getAlive() */
    public static final Integer DEFAULT_EXECUTOR_ALIVE = (int) TimeUnit.SECONDS.toMillis(60);
    /** @see ExecutorConfig#getQueueSize() */
    public static final Integer DEFAULT_EXECUTOR_QUEUE_SIZE = 256;


    //--------------------------------------------------------------------server
    /** @see ServerConfig#getHost() */
    public static final String DEFAULT_SERVER_HOST = NetUtils.getLocalhost4Ip();
    /** @see ServerConfig#getPort() */
    public static final Integer DEFAULT_SERVER_PORT = 12888;
    /** @see ServerConfig#getExecutor() */
    public static final ExecutorConfig DEFAULT_SERVER_EXECUTOR = ExecutorConfig.fix()
            .corePoolSize(SysUtils.DOUBLE_CPU)
            .maxPoolSize(SysUtils.DOUBLE_CPU)
            .queueSize(1024);

    static {
        DEFAULT_SERVER_EXECUTOR.initDefaultConfig();
    }


    //--------------------------------------------------------------------reference
    /** 默认cluster */
    public static final String DEFAULT_REFERENCE_CLUSTER = ClusterType.FAIL_FAST.getName();
    /** 默认负载均衡类型 */
    public static final String DEFAULT_REFERENCE_LOADBALANCE = LoadBalanceType.ROUND_ROBIN.getName();
    /** 默认路由类型 */
    public static final String DEFAULT_REFERENCE_ROUTER = RouterType.NONE.getName();
    /** 默认不使用泛化接口 */
    public static final Boolean DEFAULT_REFERENCE_GENERIC = false;
    /** 默认服务引用bootstrap 类型 */
    public static final String DEFAULT_REFERENCE_BOOTSTRAP = BootstrapType.DEFAULT.getName();


    //--------------------------------------------------------------------service
    /** 默认服务权重 */
    public static final Integer DEFAULT_SERVICE_WEIGHT = 1;
    /** 默认服务bootstrap 类型 */
    public static final String DEFAULT_SERVICE_BOOTSTRAP = BootstrapType.DEFAULT.getName();
    /** 默认延迟发布时间 */
    public static final Long DEFAULT_SERVICE_DELAY = 0L;
    /** 默认同步发布 */
    public static final Boolean DEFAULT_SERVICE_EXPORT_ASYNC = false;
    /** 默认注册到注册中心 */
    public static final Boolean DEFAULT_SERVICE_REGISTER = true;

    //--------------------------------------------------------------------registry
    /** 默认注册中心优先级 */
    public static final Boolean DEFAULT_REGISTRY_PREFERRED = false;
    /** 默认注册中心区域 */
    public static final String DEFAULT_REGISTRY_ZONE = "";
    /** 默认注册中心权重 */
    public static final Integer DEFAULT_REGISTRY_WEIGHT = 1;

    //--------------------------------------------------------------------cluster
    //--------------------------------------------------------------------failback cluster
    /** failback cluster默认重试次数 */
    public static final int DEFAULT_FAILBACK_RETRIES = 3;
    /** failback cluster默认最大重试任务数量 */
    public static final int DEFAULT_FAILBACK_RETRY_TASK = 100;

    //--------------------------------------------------------------------forking cluster
    /** forking cluster默认并发次数 */
    public static final int DEFAULT_FORKING_FORKS = 2;
    //--------------------------------------------------------------------broadcast cluster
    /** broadcast cluster默认服务调用失败的比例 */
    public static final int DEFAULT_BROADCAST_FAIL_PERCENT = 20;

    private DefaultConfig() {
    }
}
