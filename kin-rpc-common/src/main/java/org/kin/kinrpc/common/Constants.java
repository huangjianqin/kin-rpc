package org.kin.kinrpc.common;


/**
 * Created by 健勤 on 2016/10/9.
 */
public class Constants {
    private Constants() {

    }

    //url参数
    public static final int URL_PARAM_NUM = 6;
    public static final String APP_NAME_KEY = "appName";
    public static final String SERVICE_NAME_KEY = "serviceName";
    public static final String VERSION_KEY = "version";
    //发送方支持压缩, 接收方根据实际解压后接受或直接接受
    public static final String COMPRESSION_KEY = "compression";
    //连接超时
    public static final String TIMEOUT_KEY = "timeout";
    public static final String REGISTRY_KEY = "registry";
    public static final String REGISTRY_URL_KEY = "registryURL";
    //注册中心会话超时
    public static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    public static final String RETRY_TIMES_KEY = "retryTimes";
    public static final String RETRY_TIMEOUT_KEY = "retryTimeout";
    public static final String SERIALIZE_KEY = "serialize";
    public static final String ROUTER_KEY = "router";
    public static final String LOADBALANCE_KEY = "loadbalance";
    public static final String BYTE_CODE_INVOKE_KEY = "byteCodeInvoke";

    //    public static final String CLAXX_KEY = "claxx";//接口名
//    public static final String VERSION_KEY = "version";//服务版本
//    public static final String METHOD_KEY = "method";//接口方法列表
//    public static final String CALLED_METHOD_KEY = "calledMethod";//被调用方法,参数不显示
    //启动检查
//    public static final String CHECK_KEY = "check";


    public static final String KINRPC_PROTOCOL = "kinrpc";
    //zookeeper注册中心会话超时
    public static final int ZOOKEEPER_SESSION_TIMEOUT = 5000;
    //provider Server默认配置
    public static final int SERVER_DEFAULT_PORT = 16888;
    //directURLs register url splitor
    public static final String DIRECT_URLS_REGISTRY_SPLITOR = ";";
    //等待重试时间
    public static final int RETRY_TIMEOUT = 500;
    //限流
    //每秒n次
    public static final int PROVIDER_REQUEST_THRESHOLD = 5;
    //每秒n次
    public static final int REFERENCE_REQUEST_THRESHOLD = 5;
    //每秒所有channel请求访问量Byte
    public static final int SERVER_REQUEST_THRESHOLD = 20000;


    //Reference默认配置
    public static final int REFERENCE_DEFAULT_CONNECT_TIMEOUT = 5000;
}