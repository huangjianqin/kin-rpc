package org.kin.kinrpc.transport;

/**
 * 重连response超时异常
 *
 * @author huangjianqin
 * @date 2023/6/21
 */
public class ReconnectTimeoutException extends RuntimeException {
    private static final long serialVersionUID = -839458057007316525L;

    /** 单例 */
    public static final ReconnectTimeoutException INSTANCE = new ReconnectTimeoutException();

    private ReconnectTimeoutException() {
    }
}
