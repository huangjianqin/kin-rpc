package org.kin.kinrpc.transport;

/**
 * 心跳response超时异常
 *
 * @author huangjianqin
 * @date 2023/6/21
 */
public class HeartbeatTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 3722114306279747503L;
    /** 单例 */
    public static final HeartbeatTimeoutException INSTANCE = new HeartbeatTimeoutException();

    private HeartbeatTimeoutException() {
    }
}
