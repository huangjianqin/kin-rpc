package org.kin.kinrpc.common;


/**
 * Created by 健勤 on 2016/10/9.
 */
public final class Constants {
    //Zookeeper规则
    //格式/kinrpc/{serviceName}/{host:port}
    public static final int ZOOKEEPER_REGISTRY_DEFAULT_PORT = 2181;
    //连接注册中心的会话超时,以毫秒算
    public static final int ZOOKEEPER_REGISTRY_DEFAULT_SESSIONTIMEOUT = 5000;

    //url参数
    public static final String KINP_URL_HEARDER = "kin://";
    public static final String URL_HOST_PARAMETER_SEPARATOR = "/";
    public static final String URL_KEY_VALUE_SEPARATOR = "=";
    public static final String URL_PARAMETER_SEPARATOR = "&";
    public static final String CHECK_KEY = "check";//启动检查
    public static final String TIMEOUT_KEY = "timeout";//连接超时
    public static final String SESSION_KEY = "sessionTimeOut";//会话超时
    public static final String CLAXX_KEY = "claxx";//接口名
    public static final String VERSION_KEY = "version";//服务版本
    public static final String METHOD_KEY = "method";//接口方法列表
    public static final String CALLED_METHOD_KEY = "calledMethod";//被调用方法,参数不显示

    //provider Server默认配置
    public static final int SERVER_DEFAULT_PORT = 16888;
    public static final int SERVER_DEFAULT_THREADNUM = 4;
    //限流
    public static final int SERVER_REQUEST_THRESHOLD = 100;

    //Reference默认配置
    public static final int REFERENCE_DEFAULT_CONNECT_TIMEOUT = 5000;
    public static final int DIRECTORY_DEFAULT_INIT_THREADNUM = 4;
    public static final int DIRECTORY_DEFAULT_MAX_THREADNUM = 32;
    public static final long DIRECTORY_DEFAULT_THREADS_ALIVE = 300L;

    private Constants() throws IllegalAccessException {
        throw new IllegalAccessException();
    }
}