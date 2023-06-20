package org.kin.kinrpc.constants;


/**
 * 通用常量
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class Constants {
    /** server默认端口 */
    public static final int DEFAULT_SERVER_PORT = 16888;

    private Constants() {
    }


    // TODO: 2023/6/18 移除'
    //    //-------------------------------------------------配置key--------------------------------------------------------
//    /** url参数 */
//    int URL_PARAM_NUM = 6;
//    /** 应用名 */
//    String APP_KEY = "app";
//    /** 服务所属组 */
//    String GROUP_KEY = "group";
//    /** 服务名 */
//    String SERVICE_KEY = "service";
//    /** 版本号 */
//    String VERSION_KEY = "version";
//    /** 发送方支持压缩, 接收方根据实际解压后接受或直接接受 */
//    String COMPRESSION_KEY = "compression";
//    /** 服务provider是否支持并发 */
//    String PARALLELISM_KEY = "parallelism";
//    /** 服务provider流控 or 限流 */
//    String TPS_KEY = "tps";
//    /** 注册中心类型 */
//    String REGISTRY_KEY = "registry";
//    /** 注册中心地址 */
//    String REGISTRY_URL_KEY = "registryURL";
//    /** zookeeper注册中心会话超时 */
//    String SESSION_TIMEOUT_KEY = "sessionTimeout";
//    /** 重试次数 */
//    String RETRY_TIMES_KEY = "retryTimes";
//    /** 重试等待时间(即两次重试间隔时间) */
//    String RETRY_INTERVAL_KEY = "retryTimeout";
//    /** 序列化方式 */
//    String SERIALIZATION_KEY = "serialization";
//    /** 路由策略 */
//    String ROUTER_KEY = "router";
//    /** 负载均衡策略 */
//    String LOADBALANCE_KEY = "loadbalance";
//    /** 是否使用字节码增加技术 */
//    String BYTE_CODE_INVOKE_KEY = "byteCodeInvoke";
//    /** 定时轮询注册中心间隔, 目前只有redis注册中心用到 */
//    String WATCH_INTERVAL_KEY = "watchInterval";
//    /** rpc调用方法名 */
//    String INTERFACE_KEY = "interface";
//    /** async rpc call标识 */
//    String ASYNC_KEY = "async";
//    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
//    String GENERIC_KEY = "useGeneric";
//    /** rpc call等待时间(ms) */
//    String CALL_TIMEOUT_KEY = "callTimeout";
//    /** 是否允许开启ssl */
//    String SSL_ENABLED_KEY = "ssl";
//    /** invoker weight todo 赋值 */
//    String WEIGHT = "weight";
//
//    //<<<netty相关>>> 可用于kinrpc协议和grpc协议
//    /** 连接超时 */
//    String NETTY_CONNECT_TIMEOUT_KEY = "netty.connectTimeout";
//    /** socket直接发送消息, 不缓存 */
//    String NETTY_NODELAY_KEY = "netty.nodelay";
//    /** 保持连接活跃 */
//    String NETTY_KEEPALIVE_KEY = "netty.keepalive";
//    /** socket receive buffer大小 */
//    String NETTY_RCVBUF_KEY = "netty.rcvbuf";
//    /** socket send buffer大小 */
//    String NETTY_SNDBUF_KEY = "netty.sndbuf";
//    /** socket backlog, 相当于新连接数, 大于这个值时, 内核会拒绝新连接, 对程序支持的连接数没有影响, 只影响accept的数量 */
//    String NETTY_BACKLOG_KEY = "netty.backlog";
//    /** 复用地址 */
//    String NETTY_REUSEADDR_KEY = "netty.reuseaddr";
//    /** 关闭socket的延迟时间 */
//    String NETTY_LINGER_KEY = "netty.linger";
//    /** 控制socket channel write速率, 如果water mark过高, 会导致channel不可写, 同时占用内存高 */
//    String NETTY_WRITE_BUFF_WATER_MARK_KEY = "netty.writeBuffWaterMark";
//
//    //<<<kinrpc相关>>>
//    /** netty server业务线程池类型 */
//    String EXECUTOR_KEY = "executor";
//    /** netty server业务线程池核心线程数 */
//    String CORES_KEY = "cores";
//    /** netty server业务线程池最大线程数 */
//    String MAX_THREADS_KEY = "maxThreads";
//    /** netty server业务线程池等待task队列大小 */
//    String QUEUES_KEY = "queues";
//    /** netty server业务线程池线程存活时间, ms */
//    String ALIVE_KEY = "alive";
//
//    //<<<grpc相关>>>
//    /** Inbound消息大小 */
//    String GRPC_MAX_INBOUND_MESSAGE_SIZE_KEY = "grpc.maxInboundMessageSize";
//    /** Inbound元数据大小 */
//    String GRPC_MAX_INBOUND_METADATA_SIZE_KEY = "grpc.maxInboundMetaDataSize";
//    /** 流控窗口 */
//    String GRPC_FLOWCONTROL_WINDOW_KEY = "grpc.flowControlWindow";
//    /** 每个连接最大请求并发处理 */
//    String GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION_KEY = "grpc.maxConcurrentCallsPerConnection";
//
//
//    //-------------------------------------------------配置value--------------------------------------------------------
//    /** 注册中心会话超时(ms) */
//    long SESSION_TIMEOUT = 5000;
//    /** provider Server默认配置 */
//    int SERVER_DEFAULT_PORT = 16888;
//    /** directURLs register url splitor */
//    String DIRECT_URLS_REGISTRY_SPLITOR = ";";
//    /** 重试等待时间(即两次重试间隔时间)(ms) */
//    int RETRY_INTERVAL = 500;
//    /**
//     * provide默认流控
//     * 每秒n次
//     */
//    int PROVIDER_DEFAULT_TPS = Integer.MAX_VALUE;
//    /**
//     * reference默认限流
//     * 每秒n次
//     */
//    int REFERENCE_DEFAULT_TPS = Integer.MAX_VALUE;
//    /**
//     * 每秒所有请求访问量
//     * 全局
//     */
//    int REQUEST_THRESHOLD = Integer.MAX_VALUE;
//    /** 轮询redis注册中心间隔默认值(30s), 目前只有redis注册中心用到 */
//    long WATCH_INTERVAL = 30000;
//
//    /** Reference默认连接超时配置 */
//    int DEFAULT_CONNECT_TIMEOUT = 5000;
//
//    /** rpc call等待时间(ms) */
//    int RPC_CALL_TIMEOUT = 500;
//
//    /** 默认权重 */
//    int DEFAULT_WEIGHT = 1;
//
//    //<<<kinrpc相关>>>
//    /** 默认netty server业务线程池核心线程数 */
//    int CORES = SysUtils.getSuitableThreadNum();
//    /** 默认netty server业务线程池最大线程数 */
//    int MAX_THREADS = Integer.MAX_VALUE;
//    /** 默认netty server业务线程池等待task队列大小 */
//    int QUEUES = 0;
//    /** 默认netty server业务线程池线程存活时间, ms */
//    int ALIVE = 60_000;
//
//    //-------------------------------------------------传输协议相关--------------------------------------------------------
//    String GENERIC = "generic";
}