package org.kin.kinrpc.rpc.common.constants;

/**
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ReferenceConstants {
    /** reference默认重试次数 */
    public static final int DEFAULT_RETRY_TIMES = 3;
    /** reference默认rpc call timeout, ms */
    public static final int DEFAULT_RPC_CALL_TIMEOUT = 3000;

    private ReferenceConstants() {
    }
}
