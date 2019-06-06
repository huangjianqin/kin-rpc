package org.kin.kinrpc.transport.rpc;

import org.kin.kinrpc.transport.rpc.domain.RPCRequest;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public interface RPCRequestHandler {
    void handleRequest(RPCRequest rpcRequest);
}
