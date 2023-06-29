package org.kin.kinrpc.protocol;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;

/**
 * @author huangjianqin
 * @date 2023/6/28
 */
public abstract class AbstractProtocol implements Protocol {
    /**
     * server cache
     * key -> listen address
     */
    private final ReferenceCountedCache<String, RemotingServerContext> serverContextCache = new ReferenceCountedCache<>((k, c) -> c.getServer().shutdown());
    /**
     * client cache
     * key -> remote address
     */
    private final ReferenceCountedCache<String, RemotingClient> clientCache = new ReferenceCountedCache<>((k, c) -> c.shutdown());

    @Override
    public final <T> Exporter<T> export(RpcService<T> rpcService, ServerConfig serverConfig) {
        String address = serverConfig.getAddress();
        RemotingServerContext serverContext = serverContextCache.get(address, () -> createServerContext(serverConfig));
        serverContext.getRpcRequestProcessor().register(rpcService);
        return new Exporter<T>() {
            @Override
            public RpcService<T> service() {
                return rpcService;
            }

            @Override
            public void unexport() {
                serverContext.getRpcRequestProcessor().unregister(rpcService.serviceId());
                serverContextCache.release(address);
            }
        };
    }

    /**
     * 构造{@link RemotingServerContext}实例
     *
     * @param serverConfig server config
     * @return {@link RemotingServerContext}实例
     */
    private RemotingServerContext createServerContext(ServerConfig serverConfig) {
        RemotingServer server = createServer(serverConfig);
        DefaultRpcRequestProcessor rpcRequestProcessor = new DefaultRpcRequestProcessor();
        server.registerRequestProcessor(rpcRequestProcessor);

        server.start();
        return new RemotingServerContext(server, rpcRequestProcessor);
    }

    /**
     * 构造{@link RemotingServer}实例
     *
     * @param serverConfig server config
     * @return {@link RemotingServer}实例
     */
    protected abstract RemotingServer createServer(ServerConfig serverConfig);

    @Override
    public final <T> ReferenceInvoker<T> refer(ServiceInstance instance, SslConfig sslConfig) {
        String address = instance.address();
        RemotingClient client = clientCache.get(address, () -> {
            RemotingClient innerClient = createClient(instance, sslConfig);
            innerClient.connect();
            return innerClient;
        });
        return new DefaultReferenceInvoker<>(instance, client, (inst, cli) -> clientCache.release(address));
    }

    /**
     * 构造{@link RemotingClient}实例
     *
     * @param instance service instance
     * @return {@link RemotingClient}实例
     */
    protected abstract RemotingClient createClient(ServiceInstance instance, SslConfig sslConfig);

    @Override
    public void destroy() {
        serverContextCache.clear();
        clientCache.clear();
    }
}
