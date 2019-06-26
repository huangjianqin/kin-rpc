package org.kin.kinrpc.common;


/**
 * Created by 健勤 on 2016/10/9.
 */
public class Constants {
    private Constants(){

    }

    //url参数
    public static final int URL_PARAM_NUM = 6;
    public static final String APP_NAME_KEY = "appName";
    public static final String SERVICE_NAME_KEY = "serviceName";
    //连接超时
    public static final String TIMEOUT_KEY = "timeout";
    public static final String REGISTRY_KEY = "registry";
    public static final String REGISTRY_URL_KEY = "registryURL";
    public static final String REGISTRY_PASSWORD_KEY = "registryPassword";
    //会话超时
    public static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    public static final String RETRY_TIMES_KEY = "retryTimes";
    public static final String RETRY_TIMEOUT_KEY = "retryTimeout";
    public static final String SERIALIZE_KEY = "serialize";
//    public static final String CLAXX_KEY = "claxx";//接口名
//    public static final String VERSION_KEY = "version";//服务版本
//    public static final String METHOD_KEY = "method";//接口方法列表
//    public static final String CALLED_METHOD_KEY = "calledMethod";//被调用方法,参数不显示
    //启动检查
//    public static final String CHECK_KEY = "check";



    public static final String KINRPC_PROTOCOL = "kinrpc";
    //url可选值, 或默认值
    public static final String ZOOKEEPER_REGISTRY = "zookeeper";
    public static final String DEFAULT_REGISTRY = "default";
    //provider Server默认配置
    public static final int SERVER_DEFAULT_PORT = 16888;
    //default register url splitor
    public static final String DEFAULT_REGISTRY_URL_SPLITOR = ";";
    //重试次数
    public static int RETRY_TIMES = 3;
    //等待重试时间
    public static int RETRY_TIMEOUT = 500;
    //提供的序列化方式
    public static final String JAVA_SERIALIZE = "java";
    public static final String KRYO_SERIALIZE = "kryo";
    public static final String HESSION_SERIALIZE = "hession";
    //TODO 限流
    public static final int SERVER_REQUEST_THRESHOLD = 100;



    //Reference默认配置
    public static final int REFERENCE_DEFAULT_CONNECT_TIMEOUT = 5000;
}