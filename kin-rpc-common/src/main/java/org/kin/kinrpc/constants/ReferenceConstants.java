package org.kin.kinrpc.constants;

/**
 * reference端常量
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ReferenceConstants {
    /** reference config provideBy字段分隔符 */
    public static final String PROVIDE_BY_SEPARATOR = ",";

    //----------------------------------------------------------------invocation attachment key
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

    private ReferenceConstants() {
    }
}
