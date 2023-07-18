package org.kin.kinrpc.protocol;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.executor.ExecutorHelper;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingClientStateObserver;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.Transport;
import org.kin.kinrpc.transport.cmd.RequestCommand;

import java.util.Collection;
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
    private final ReferenceCountedCache<String, RemotingServerContext> serverContextCache = new ReferenceCountedCache<>((k, c) -> c.shutdown());
    /**
     * client cache, 复用client
     * key -> remote address
     * todo 同一remote server, 但一个reference需要使用ssl, 另外一个reference需要不使用ssl, 怎么处理
     * todo 同一remote server, 是否考虑需要client池
     */
    private final ReferenceCountedCache<String, DefaultReferenceInvoker<?>> referenceInvokerCache = new ReferenceCountedCache<>();

    /**
     * 返回协议名
     *
     * @return 协议名
     */
    protected abstract String name();

    @Override
    public final <T> Exporter<T> export(RpcService<T> rpcService, ServerConfig serverConfig) {
        String address = serverConfig.getAddress();
        RemotingServerContext serverContext = serverContextCache.get(address, () -> createServerContext(serverConfig));
        serverContext.register(rpcService);
        onExport(rpcService, serverContext.getServer());
        return new Exporter<T>() {
            @Override
            public RpcService<T> service() {
                return rpcService;
            }

            @Override
            public void unExport() {
                serverContext.unregister(rpcService.serviceId());
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

        Transport transport = ExtensionLoader.getExtension(Transport.class, name());
        RemotingServer server = transport.createServer(serverConfig.getHost(), serverConfig.getPort(), executor, serverConfig.getSsl());
        DefaultRpcRequestProcessor rpcRequestProcessor = new DefaultRpcRequestProcessor();
        server.registerRequestProcessor(rpcRequestProcessor);

        server.start();
        return new RemotingServerContext(server, rpcRequestProcessor);
    }

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

    @SuppressWarnings("unchecked")
    @Override
    public final <T> ReferenceInvoker<T> refer(ReferenceConfig<T> referenceConfig,
                                               ServiceInstance instance) {
        String address = instance.address();
        return (ReferenceInvoker<T>) referenceInvokerCache.get(address, () -> {
            Transport transport = ExtensionLoader.getExtension(Transport.class, name());
            RemotingClient client = wrapClient(transport.createClient(instance.host(), instance.port(), referenceConfig.getSsl()), address);
            client.connect();
            return new DefaultReferenceInvoker<>(instance, client);
        });
    }

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
            public String remoteAddress() {
                return client.remoteAddress();
            }

            @Override
            public void shutdown() {
                if (referenceInvokerCache.release(address)) {
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

            @Override
            public void addObservers(Collection<RemotingClientStateObserver> observers) {
                client.addObservers(observers);
            }
        };
    }

    @Override
    public void destroy() {
        serverContextCache.clear();
        referenceInvokerCache.clear();
    }
}
