package org.kin.kinrpc.protocol;

import org.kin.kinrpc.transport.RemotingServer;

/**
 * remoting server上下文
 *
 * @author huangjianqin
 * @date 2023/6/28
 */
public class RemotingServerContext {
    /** remoting server */
    private final RemotingServer server;
    /** rpc request processor */
    private final DefaultRpcRequestProcessor rpcRequestProcessor;

    public RemotingServerContext(RemotingServer server, DefaultRpcRequestProcessor rpcRequestProcessor) {
        this.server = server;
        this.rpcRequestProcessor = rpcRequestProcessor;
    }

    //getter
    public RemotingServer getServer() {
        return server;
    }

    public DefaultRpcRequestProcessor getRpcRequestProcessor() {
        return rpcRequestProcessor;
    }
}
