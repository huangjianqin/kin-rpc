package org.kin.kinrpc.protocol;

import org.kin.kinrpc.RpcService;
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

    /**
     * register rpc service to server
     *
     * @param rpcService {@link RpcService}实例
     */
    public void register(RpcService<?> rpcService) {
        //注册元数据
        rpcRequestProcessor.register(rpcService);
    }

    /**
     * unregister rpc service from server
     *
     * @param serviceId 服务唯一id
     */
    public void unregister(int serviceId) {
        rpcRequestProcessor.unregister(serviceId);
    }

    /**
     * remoting server shutdown
     */
    public void shutdown() {
        server.shutdown();
    }

    //getter
    public RemotingServer getServer() {
        return server;
    }

    public DefaultRpcRequestProcessor getRpcRequestProcessor() {
        return rpcRequestProcessor;
    }
}
