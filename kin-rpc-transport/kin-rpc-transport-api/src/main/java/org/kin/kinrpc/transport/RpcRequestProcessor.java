package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RpcRequestCommand;

/**
 * 内置的特殊的{@link RequestProcessor}实现, 处理{@link RpcRequestCommand}请求
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class RpcRequestProcessor implements RequestProcessor<RpcRequestCommand>{
    @Override
    public final String interest() {
        return RequestProcessor.RPC_REQUEST_INTEREST;
    }
}
