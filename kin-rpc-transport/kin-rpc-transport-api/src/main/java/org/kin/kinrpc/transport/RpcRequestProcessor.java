package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RpcRequestCommand;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class RpcRequestProcessor implements RequestProcessor<RpcRequestCommand>{
    @Override
    public final String interest() {
        return RequestProcessor.RPC_REQUEST_INTEREST;
    }
}
