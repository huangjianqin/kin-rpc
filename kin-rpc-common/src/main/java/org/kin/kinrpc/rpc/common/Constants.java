package org.kin.kinrpc.rpc.common;


/**
 * Created by 健勤 on 2016/10/9.
 */
public class Constants {
    private Constants() {

    }

    //-------------------------------------------------配置key--------------------------------------------------------
    /** url参数 */
    public static final int URL_PARAM_NUM = 6;
    /** 应用名 */
    public static final String APP_NAME_KEY = "appName";
    /** 服务名 */
    public static final String SERVICE_NAME_KEY = "serviceName";
    /** 版本号 */
    public static final String VERSION_KEY = "version";
    /** 发送方支持压缩, 接收方根据实际解压后接受或直接接受 */
    public static final String COMPRESSION_KEY = "compression";
    /** 服务provider是否支持并发 */
    public static final String PARALLELISM_KEY = "parallelism";
    /** 服务provider流控 or 限流 */
    public static final String TPS_KEY = "tps";
    /** 注册中心类型 */
    public static final String REGISTRY_KEY = "registry";
    /** 注册中心地址 */
    public static final String REGISTRY_URL_KEY = "registryURL";
    /** zookeeper注册中心会话超时 */
    public static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    /** 重试次数 */
    public static final String RETRY_TIMES_KEY = "retryTimes";
    /** 重试等待时间(即两次重试间隔时间) */
    public static final String RETRY_INTERVAL_KEY = "retryTimeout";
    /** 序列化方式 */
    public static final String SERIALIZE_KEY = "serialize";
    /** 路由策略 */
    public static final String ROUTER_KEY = "router";
    /** 负载均衡策略 */
    public static final String LOADBALANCE_KEY = "loadbalance";
    /** 是否使用字节码增加技术 */
    public static final String BYTE_CODE_INVOKE_KEY = "byteCodeInvoke";
    /** 定时轮询注册中心间隔, 目前只有redis注册中心用到 */
    public static final String WATCH_INTERVAL_KEY = "watchInterval";
    /** rpc调用方法名 */
    public static final String INTERFACE_KEY = "interface";
    /** async rpc call标识 */
    public static final String ASYNC_KEY = "async";
    /** 兼容协议(非kinrpc)是否使用Generic通用接口服务 */
    public static final String GENERIC_KEY = "useGeneric";
    /** rpc call等待时间(ms) */
    public static final String CALL_TIMEOUT_KEY = "callTimeout";
    /** 是否允许开启ssl */
    public static final String SSL_ENABLED_KEY = "ssl";

    //<<<netty相关>>> 可用于kinrpc协议和grpc协议
    /** 连接超时 */
    public static final String NETTY_CONNECT_TIMEOUT_KEY = "netty.connectTimeout";
    /** socket直接发送消息, 不缓存 */
    public static final String NETTY_NODELAY = "netty.nodelay";
    /** 保持连接活跃 */
    public static final String NETTY_KEEPALIVE = "netty.keepalive";
    /** socket receive buffer大小 */
    public static final String NETTY_RCVBUF = "netty.rcvbuf";
    /** socket send buffer大小 */
    public static final String NETTY_SNDBUF = "netty.sndbuf";
    /** socket backlog, 相当于新连接数, 大于这个值时, 内核会拒绝新连接, 对程序支持的连接数没有影响, 只影响accept的数量 */
    public static final String NETTY_BACKLOG = "netty.backlog";
    /** 复用地址 */
    public static final String NETTY_REUSEADDR = "netty.reuseaddr";
    /** 关闭socket的延迟时间 */
    public static final String NETTY_LINGER = "netty.linger";
    /** 控制socket channel write速率, 如果water mark过高, 会导致channel不可写, 同时占用内存高 */
    public static final String NETTY_WRITE_BUFF_WATER_MARK = "netty.writeBuffWaterMark";

    //<<<grpc相关>>>
    /** Inbound消息大小 */
    public static final String GRPC_MAX_INBOUND_MESSAGE_SIZE_KEY = "grpc.maxInboundMessageSize";
    /** Inbound元数据大小 */
    public static final String GRPC_MAX_INBOUND_METADATA_SIZE_KEY = "grpc.maxInboundMetaDataSize";
    /** 流控窗口 */
    public static final String GRPC_FLOWCONTROL_WINDOW_KEY = "grpc.flowControlWindow";
    /** 每个连接最大请求并发处理 */
    public static final String GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION_KEY = "grpc.maxConcurrentCallsPerConnection";


    //-------------------------------------------------配置value--------------------------------------------------------
    /** 注册中心会话超时(ms) */
    public static final long SESSION_TIMEOUT = 5000;
    /** provider Server默认配置 */
    public static final int SERVER_DEFAULT_PORT = 16888;
    /** directURLs register url splitor */
    public static final String DIRECT_URLS_REGISTRY_SPLITOR = ";";
    /** 重试等待时间(即两次重试间隔时间)(ms) */
    public static final int RETRY_INTERVAL = 50;
    /**
     * provide默认流控
     * 每秒n次
     */
    public static final int PROVIDER_DEFAULT_TPS = Integer.MAX_VALUE;
    /**
     * reference默认限流
     * 每秒n次
     */
    public static final int REFERENCE_DEFAULT_TPS = Integer.MAX_VALUE;
    /**
     * 每秒所有请求访问量
     * 全局
     */
    public static final int REQUEST_THRESHOLD = Integer.MAX_VALUE;
    /** 轮询redis注册中心间隔默认值(30s), 目前只有redis注册中心用到 */
    public static final long WATCH_INTERVAL = 30000;

    /** Reference默认连接超时配置 */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /** rpc call等待时间(ms) */
    public static final int RPC_CALL_TIMEOUT = 500;

    //-------------------------------------------------传输协议相关--------------------------------------------------------
    public static final String GENERIC = "generic";
}