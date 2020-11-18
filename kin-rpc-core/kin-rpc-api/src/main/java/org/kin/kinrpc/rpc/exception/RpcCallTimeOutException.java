package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2020/11/18
 */
public class RpcCallTimeOutException extends RuntimeException {
    private static final long serialVersionUID = 490253965471277042L;

    public RpcCallTimeOutException(String errorMsg) {
        super(errorMsg);
    }

    public RpcCallTimeOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
