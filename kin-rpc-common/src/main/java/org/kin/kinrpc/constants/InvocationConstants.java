package org.kin.kinrpc.constants;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
public final class InvocationConstants {
    //----------------------------------------------------------------common invocation attachment key
    /** 是否开启服务方法调用参数校验 */
    public static final String VALIDATION_KEY = "validation";

    //----------------------------------------------------------------reference invocation attachment key
    /** cluster根据规则策略选择可用的invoker */
    public static final String SELECTED_INVOKER_KEY = "selected";
    /** 服务方法配置 */
    public static final String METHOD_CONFIG_KEY = "methodConfig";
    /** 序列化类型配置 */
    public static final String SERIALIZATION_KEY = "serialization";
    /** rpc call start time(ms) */
    public static final String RPC_CALL_START_TIME_KEY = "rpcCallStartTime";
    /** rpc call Finish time(ms) */
    public static final String RPC_CALL_FINISH_TIME_KEY = "rpcCallFinishTime";
    /** loadbalance, 可能为null */
    public static final String LOADBALANCE = "loadbalance";
    /** filter chain */
    public static final String FILTER_CHAIN = "filterChain";

    //----------------------------------------------------------------service invocation attachment key
    /** service token */
    public static final String TOKEN_KEY = "token";

    private InvocationConstants() {
    }
}
