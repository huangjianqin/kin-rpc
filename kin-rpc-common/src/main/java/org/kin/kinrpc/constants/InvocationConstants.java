package org.kin.kinrpc.constants;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
public final class InvocationConstants {
    //----------------------------------------------------------------common invocation attachment key
    /** 是否开启服务方法调用参数校验 */
    public static final String VALIDATION_KEY = "validation";
    /** fallback class(service) name */
    public static final String FALLBACK_KEY = "fallback";
    /** application */
    public static final String APPLICATION_KEY = "application";
    /** 真正执行服务方法调用的invoker */
    public static final String RPC_CALL_INVOKER_KEY = "rpcCallInvoker";

    //----------------------------------------------------------------reference invocation attachment key
    /** 服务方法配置 */
    public static final String METHOD_CONFIG_KEY = "methodConfig";
    /** 序列化类型配置 */
    public static final String SERIALIZATION_KEY = "serialization";
    /** rpc call start time(ms) */
    public static final String RPC_CALL_START_TIME_KEY = "rpcCallStartTime";
    /** rpc call end time(ms) */
    public static final String RPC_CALL_END_TIME_KEY = "rpcCallEndTime";
    /** loadbalance, 可能为null */
    public static final String LOADBALANCE_KEY = "loadbalance";
    /** registry zone */
    public static final String REGISTRY_ZONE_KEY = "registryZone";
    /** registry zone force */
    public static final String REGISTRY_ZONE_FORCE_KEY = "registryZoneForce";
    /** 服务引用配置 */
    public static final String REFERENCE_CONFIG_KEY = "referenceConfig";

    //----------------------------------------------------------------service invocation attachment key
    /** service token */
    public static final String TOKEN_KEY = "token";
    /** rpc call timeout(absolutely end time) */
    public static final String TIMEOUT_KEY = "timeout";
    /** 服务配置 */
    public static final String SERVICE_CONFIG_KEY = "serviceConfig";

    private InvocationConstants() {
    }
}
