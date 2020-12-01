package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class RpcCallErrorException extends RuntimeException {
    private static final long serialVersionUID = -2166664244857494642L;

    public RpcCallErrorException(String errorMsg) {
        super(errorMsg);
    }

    public RpcCallErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcCallErrorException(Throwable cause) {
        super("", cause);
    }
}
