package org.kin.kinrpc.protocol;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ExecutorHelper;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.cmd.RequestCommand;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
     * client cache, 复用client
     * key -> remote address
     * todo 同一remote server, 但一个reference需要使用ssl, 另外一个reference需要不使用ssl, 怎么处理
     * todo 同一remote server, 是否考虑需要client池
     */
    private final ReferenceCountedCache<String, RemotingClient> clientCache = new ReferenceCountedCache<>();

    @Override
    public final <T> Exporter<T> export(RpcService<T> rpcService, ServerConfig serverConfig) {
        String address = serverConfig.getAddress();
        RemotingServerContext serverContext = serverContextCache.get(address, () -> createServerContext(serverConfig));
        serverContext.getRpcRequestProcessor().register(rpcService);
        onExport(rpcService, serverContext.getServer());
        return new Exporter<T>() {
            @Override
            public RpcService<T> service() {
                return rpcService;
            }

            @Override
            public void unExport() {
                serverContext.getRpcRequestProcessor().unregister(rpcService.serviceId());
                onUnExport(rpcService, serverContext.getServer());
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
        ExecutorConfig executorConfig = serverConfig.getExecutor();
        String executorName = serverConfig.getAddress() + "-command-processor";
        ManagedExecutor executor = Objects.nonNull(executorConfig) ? ExecutorHelper.getOrCreateExecutor(executorConfig, executorName) : null;

        RemotingServer server = createServer(serverConfig, executor);
        DefaultRpcRequestProcessor rpcRequestProcessor = new DefaultRpcRequestProcessor();
        server.registerRequestProcessor(rpcRequestProcessor);

        server.start();
        return new RemotingServerContext(server, rpcRequestProcessor);
    }

    /**
     * 构造{@link RemotingServer}实例
     *
     * @param serverConfig server config
     * @param executor     server command process executor
     * @return {@link RemotingServer}实例
     */
    protected abstract RemotingServer createServer(ServerConfig serverConfig, @Nullable ManagedExecutor executor);

    /**
     * service export时触发
     * 用于service export时, 自定义server一些操作
     */
    protected void onExport(RpcService<?> service, RemotingServer server) {
        //default do nothing
    }

    /**
     * service unExport时触发
     * 用于service unExport时, 自定义server一些操作
     */
    protected void onUnExport(RpcService<?> service, RemotingServer server) {
        //default do nothing
    }

    @Override
    public final <T> ReferenceInvoker<T> refer(ServiceInstance instance, SslConfig sslConfig) {
        String address = instance.address();
        RemotingClient client = clientCache.get(address, () -> {
            RemotingClient innerClient = wrapClient(createClient(instance, sslConfig), address);
            innerClient.connect();
            return innerClient;
        });
        return new DefaultReferenceInvoker<>(instance, client);
    }

    /**
     * 构造{@link RemotingClient}实例
     *
     * @param instance service instance
     * @return {@link RemotingClient}实例
     */
    protected abstract RemotingClient createClient(ServiceInstance instance, SslConfig sslConfig);

    /**
     * 对{@link RemotingClient#shutdown()}进一步封装, 释放client引用, 而不是直接shutdown client
     *
     * @param client  remoting client
     * @param address remote address
     * @return wrapped remoting client instance
     */
    private RemotingClient wrapClient(RemotingClient client, String address) {
        return new RemotingClient() {
            @Override
            public void connect() {
                client.connect();
            }

            @Override
            public boolean isAvailable() {
                return client.isAvailable();
            }

            @Override
            public void shutdown() {
                if (clientCache.release(address)) {
                    client.shutdown();
                }
            }

            @Override
            public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
                return client.requestResponse(command);
            }

            @Override
            public CompletableFuture<Void> fireAndForget(RequestCommand command) {
                return client.fireAndForget(command);
            }
        };
    }

    @Override
    public void destroy() {
        serverContextCache.clear();
        clientCache.clear();
    }
}
