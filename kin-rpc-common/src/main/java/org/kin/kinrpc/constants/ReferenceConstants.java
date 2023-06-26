package org.kin.kinrpc.constants;

/**
 * reference端常量
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ReferenceConstants {
    /** reference默认重试次数 */
    public static final int DEFAULT_RETRY_TIMES = 3;
    /** reference默认rpc call timeout(ms) */
    public static final int DEFAULT_RPC_CALL_TIMEOUT = 3000;

    //----------------------------------------------------------------invocation attachment key
    /** cluster根据规则策略选择可用的invoker */
    public static final String SELECTED_INVOKER_KEY = "selected";
    /** 服务方法配置 */
    public static final String METHOD_CONFIG_KEY = "methodConfig";

    private ReferenceConstants() {
    }
}
