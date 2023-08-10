package org.kin.kinrpc;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class RpcTimeoutException extends RpcException {
    private static final long serialVersionUID = 958608855038440735L;

    public RpcTimeoutException(String message) {
        super(message);
    }

    public RpcTimeoutException(Invocation invocation, int timeout) {
        super(String.format("rpc call timeout after %d ms, invocation=%s", timeout, invocation));
    }
}