package org.kin.kinrpc.rpc.future;


import org.kin.kinrpc.rpc.transport.domain.RPCResponse;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncRPCCallback {
    void success(RPCResponse rpcResponse);

    void fail(Exception e);
}
