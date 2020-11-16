package org.kin.kinrpc.rpc.common;


import java.util.concurrent.TimeUnit;

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
    public static final String RATE_KEY = "rate";
    /** 连接超时 */
    public static final String CONNECT_TIMEOUT_KEY = "connectTimeout";
    /** 注册中心类型 */
    public static final String REGISTRY_KEY = "registry";
    /** 注册中心地址 */
    public static final String REGISTRY_URL_KEY = "registryURL";
    /** zookeeper注册中心会话超时 */
    public static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    /** 重试次数 */
    public static final String RETRY_TIMES_KEY = "retryTimes";
    /** 重试超时时间 */
    public static final String RETRY_TIMEOUT_KEY = "retryTimeout";
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


    //-------------------------------------------------配置value--------------------------------------------------------
    /** 注册中心会话超时(ms) */
    public static final long SESSION_TIMEOUT = 5000;
    /** provider Server默认配置 */
    public static final int SERVER_DEFAULT_PORT = 16888;
    /** directURLs register url splitor */
    public static final String DIRECT_URLS_REGISTRY_SPLITOR = ";";
    /** 等待重试时间(ms) */
    public static final long RETRY_TIMEOUT = 50;
    /**
     * provide默认流控
     * 每秒n次
     */
    public static final int PROVIDER_REQUEST_THRESHOLD = Integer.MAX_VALUE;
    /**
     * reference默认限流
     * 每秒n次
     */
    public static final int REFERENCE_REQUEST_THRESHOLD = Integer.MAX_VALUE;
    /**
     * 每秒所有请求访问量
     * 全局
     */
    public static final int SERVER_REQUEST_THRESHOLD = Integer.MAX_VALUE;
    /** 定时轮询注册中心间隔默认值(30s), 目前只有redis注册中心用到 */
    public static final long WATCH_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    /** Reference默认配置 */
    public static final int REFERENCE_DEFAULT_CONNECT_TIMEOUT = 5000;

    //-------------------------------------------------传输协议相关--------------------------------------------------------
    public static final String GENERIC_KEY = "generic";
}