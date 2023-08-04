package org.kin.kinrpc;

/**
 * rpc call rate limit and block exception after service downgrade
 *
 * @author huangjianqin
 * @date 2023/8/2
 */
public class RpcExceptionBlockException extends RpcException {
    private static final long serialVersionUID = 8162952723829520518L;

    public RpcExceptionBlockException(String message) {
        super(message);
    }

    public RpcExceptionBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcExceptionBlockException(Throwable cause) {
        super(cause);
    }
}
