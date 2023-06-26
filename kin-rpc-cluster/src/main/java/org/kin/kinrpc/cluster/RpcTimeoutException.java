package org.kin.kinrpc.cluster;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcException;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class RpcTimeoutException extends RpcException {

    public RpcTimeoutException(Invocation invocation, int timeout) {
        super(String.format("rpc call timeout after %d ms, invocation=%s", timeout, invocation));
    }
}