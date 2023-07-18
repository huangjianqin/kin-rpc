package org.kin.kinrpc;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class RpcTimeoutException extends RpcException {

    public RpcTimeoutException(Invocation invocation, int timeout) {
        super(String.format("rpc call timeout after %d ms, invocation=%s", timeout, invocation));
    }
}