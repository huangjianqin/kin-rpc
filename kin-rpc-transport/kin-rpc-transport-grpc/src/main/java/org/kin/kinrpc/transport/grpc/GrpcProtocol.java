package org.kin.kinrpc.transport.grpc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.*;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.kin.framework.proxy.Javassists;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.common.SslConfig;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.transport.AbstractProxyProtocol;
import org.kin.kinrpc.transport.NettyUtils;
import org.kin.kinrpc.transport.grpc.interceptor.ClientInterceptor;
import org.kin.kinrpc.transport.grpc.interceptor.GrpcConfigurator;
import org.kin.kinrpc.transport.grpc.interceptor.ServerInterceptor;
import org.kin.kinrpc.transport.grpc.interceptor.ServerTransportFilter;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开发流程与grpc类似, 只是底层通信和编程模型使用的是kinrpc
 * 详情: 导入kinrpc自定义.proto代码生成器, 编译, 并获取KinRpc{ServiceName}Grpc
 * 实现KinRpc{ServiceName}Grpc.{ServiceName}ImplBase, 编写服务逻辑
 *
 * @author huangjianqin
 * @date 2020/12/1
 */
public final class GrpcProtocol extends AbstractProxyProtocol {
    /* key -> address, value -> gRPC server */
    private static final Cache<String, GrpcServer> SERVERS =
            CacheBuilder.newBuilder()
                    .removalListener(n -> ((GrpcServer) n.getValue()).close()).build();
    /* key -> address, value -> gRPC channels */
    private static final ConcurrentMap<String, ReferenceCountManagedChannel> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 因为需要获取服务实例, 所以才不实现doExport方法
     */
    @Override
    public <T> Exporter<T> export(ProviderInvoker<T> invoker) {
        Url url = invoker.url();
        Class<T> interfaceC = invoker.getInterface();
        String address = url.getAddress();

        boolean useByteCode = url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY);
        T proxy;
        if (useByteCode) {
            proxy = javassistProxyProviderInvoker(invoker, interfaceC);
        } else {
            proxy = jdkProxyProviderInvoker(invoker, interfaceC);
        }

        //grpc server
        GrpcServer grpcServer;
        synchronized (SERVERS) {
            try {
                grpcServer = SERVERS.get(address, () -> {
                    GrpcHandlerRegistry registry = new GrpcHandlerRegistry();
                    return new GrpcServer(address, builderServer(url, registry), registry);
                });
            } catch (ExecutionException e) {
                throw new RpcCallErrorException(e);
            }

            //获取服务实例
            T serivce = invoker.getSerivce();
            Class<?> serivceClass = serivce.getClass();
            try {
                Method method = serivceClass.getMethod("setProxiedImpl", interfaceC);
                method.invoke(serivce, proxy);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to set kinrpc proxied service impl to stub, please make sure your stub " +
                        "was generated by the kinrpc-protoc-compiler.", e);
            }

            grpcServer.registry.addService((BindableService) serivce, url.getServiceKey());

            if (!grpcServer.started) {
                grpcServer.start();
            }
            grpcServer.incrementAndGetCount();
        }

        info("grpc service '{}' export address '{}'", url.getServiceKey(), url.getAddress());

        return new Exporter<T>() {
            @Override
            public Invoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void unexport() {
                invoker.destroy();
                //释放无用代理类
                Javassists.detach(proxy.getClass().getName());

                synchronized (SERVERS) {
                    grpcServer.registry.removeService(url.getServiceKey());
                    grpcServer.close();
                }
            }
        };
    }

    /**
     * @return 获取开发者自定义grpc 配置
     */
    private static Optional<GrpcConfigurator> getConfigurator() {
        // Give users the chance to customize ServerBuilder
        List<GrpcConfigurator> configurators = RpcServiceLoader.LOADER.getExtensions(GrpcConfigurator.class);
        if (CollectionUtils.isNonEmpty(configurators)) {
            return Optional.of(configurators.iterator().next());
        }
        return Optional.empty();
    }

    /**
     * 构建Grpc server
     */
    @SuppressWarnings("unchecked")
    private Server builderServer(Url url, GrpcHandlerRegistry registry) {
        NettyServerBuilder builder =
                NettyServerBuilder
                        .forPort(url.getPort())
                        .fallbackHandlerRegistry(registry);

        int maxInboundMessageSize = url.getIntParam(Constants.GRPC_MAX_INBOUND_MESSAGE_SIZE_KEY);
        if (maxInboundMessageSize > 0) {
            builder.maxInboundMessageSize(maxInboundMessageSize);
        }

        int maxInboundMetadataSize = url.getIntParam(Constants.GRPC_MAX_INBOUND_METADATA_SIZE_KEY);
        if (maxInboundMetadataSize > 0) {
            builder.maxInboundMetadataSize(maxInboundMetadataSize);
        }

        if (url.getBooleanParam(Constants.SSL_ENABLED_KEY)) {
            builder.sslContext(buildServerSslContext());
        }

        int flowControlWindow = url.getIntParam(Constants.GRPC_FLOWCONTROL_WINDOW_KEY);
        if (flowControlWindow > 0) {
            builder.flowControlWindow(flowControlWindow);
        }

        int maxCalls = url.getIntParam(Constants.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION_KEY);
        if (maxCalls > 0) {
            builder.maxConcurrentCallsPerConnection(maxCalls);
        }

        //netty options
        for (Map.Entry<ChannelOption, Object> entry : NettyUtils.convert(url).entrySet()) {
            builder.withOption(entry.getKey(), entry.getValue());
        }

        // server interceptors
        List<ServerInterceptor> serverInterceptors = RpcServiceLoader.LOADER.getExtensions(ServerInterceptor.class);
        for (ServerInterceptor serverInterceptor : serverInterceptors) {
            builder.intercept(serverInterceptor);
        }

        // server filters
        List<ServerTransportFilter> transportFilters = RpcServiceLoader.LOADER.getExtensions(ServerTransportFilter.class);
        for (ServerTransportFilter transportFilter : transportFilters) {
            builder.addTransportFilter(transportFilter.grpcTransportFilter());
        }

        return getConfigurator()
                .map(configurator -> configurator.configureServerBuilder(builder, url))
                .orElse(builder)
                .build();
    }

    /**
     * 构建server ssl
     */
    private static SslContext buildServerSslContext() {
        SslConfig sslConfig = SslConfig.INSTANCE;

        SslContextBuilder sslClientContextBuilder;
        try {
            String password = sslConfig.getServerKeyPassword();
            if (password != null) {
                sslClientContextBuilder = GrpcSslContexts.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream(), password);
            } else {
                sslClientContextBuilder = GrpcSslContexts.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream());
            }

            InputStream trustCertCollectionFilePath = sslConfig.getServerTrustCertCollectionPathStream();
            if (trustCertCollectionFilePath != null) {
                sslClientContextBuilder.trustManager(trustCertCollectionFilePath);
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        }
        try {
            return sslClientContextBuilder.build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> AsyncInvoker<T> reference(Url url) throws Throwable {
        Class<T> interfaceC;
        try {
            interfaceC = (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw e;
        }

        Class<?> enclosingClass = interfaceC.getEnclosingClass();

        if (enclosingClass == null) {
            throw new IllegalArgumentException(interfaceC.getName() + " must be declared inside protobuf generated classes, " +
                    "should be something like KinRpc{ServiceName}Grpc.{ServiceName}.");
        }

        Method kinRpcStubMethod;
        try {
            kinRpcStubMethod = enclosingClass.getDeclaredMethod("getKinRpcStub", Channel.class, CallOptions.class, Url.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Does not find getKinRpcStub in " +
                            enclosingClass.getName() +
                            ", please use the customized kin-rpc-transport-grpc to update the generated classes.");
        }

        //grpc Channel
        ReferenceCountManagedChannel channel = getSharedChannel(url);

        info("grpc reference '{}' refer address '{}'", url.getService(), url.getAddress());

        //获取stub
        try {
            return generateAsyncInvoker(url, interfaceC, (T) kinRpcStubMethod.invoke(null,
                    channel,
                    buildCallOptions(url),
                    url
            ), url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY), channel::shutdown);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not create stub through reflection.", e);
        }
    }

    /**
     * 获取共用grpc channel
     */
    private ReferenceCountManagedChannel getSharedChannel(Url url) {
        String key = url.getAddress();
        ReferenceCountManagedChannel channel = CHANNEL_MAP.get(key);

        if (channel != null && !channel.isTerminated()) {
            channel.incrementAndGetCount();
            return channel;
        }

        synchronized (this) {
            channel = CHANNEL_MAP.get(key);
            // dubbo check
            if (channel != null && !channel.isTerminated()) {
                channel.incrementAndGetCount();
            } else {
                channel = new ReferenceCountManagedChannel(initChannel(url));
                CHANNEL_MAP.put(key, channel);
            }
        }

        return channel;
    }

    /**
     * 创建新的grpc channel
     */
    @SuppressWarnings("unchecked")
    private ManagedChannel initChannel(Url url) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(url.getHost(), url.getPort());
        if (url.getBooleanParam(Constants.SSL_ENABLED_KEY)) {
            builder.sslContext(buildClientSslContext());
        } else {
            builder.usePlaintext();
        }
        builder.disableRetry();
        //netty options
        for (Map.Entry<ChannelOption, Object> entry : NettyUtils.convert(url).entrySet()) {
            builder.withOption(entry.getKey(), entry.getValue());
        }

        // client interceptors
        List<io.grpc.ClientInterceptor> interceptors = new ArrayList<>(RpcServiceLoader.LOADER.getExtensions(ClientInterceptor.class));
        builder.intercept(interceptors);

        return getConfigurator()
                .map(configurator -> configurator.configureChannelBuilder(builder, url))
                .orElse(builder)
                .build();
    }

    /**
     * 构建client ssl
     */
    private static SslContext buildClientSslContext() {
        SslConfig sslConfig = SslConfig.INSTANCE;

        SslContextBuilder builder = GrpcSslContexts.forClient();
        try {
            InputStream trustCertCollectionFilePath = sslConfig.getClientTrustCertCollectionPathStream();
            if (trustCertCollectionFilePath != null) {
                builder.trustManager(trustCertCollectionFilePath);
            }
            InputStream clientCertChainFilePath = sslConfig.getClientKeyCertChainPathStream();
            InputStream clientPrivateKeyFilePath = sslConfig.getClientPrivateKeyPathStream();
            if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
                String password = sslConfig.getClientKeyPassword();
                if (password != null) {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath, password);
                } else {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or find invalid certificate.", e);
        }
        try {
            return builder.build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    /**
     * 获取grpc call options
     */
    private CallOptions buildCallOptions(Url url) {
        CallOptions callOptions = CallOptions.DEFAULT;
        return getConfigurator()
                .map(configurator -> configurator.configureCallOptions(callOptions, url))
                .orElse(callOptions);
    }

    @Override
    protected <T> Runnable doExport(T proxyedInvoker, Class<T> interfaceC, Url url) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected <T> T doReference(Class<T> interfaceC, Url url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        SERVERS.invalidateAll();
        for (ReferenceCountManagedChannel channel : CHANNEL_MAP.values()) {
            channel.shutdown();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * grpc server信息
     */
    private class GrpcServer {
        /** server池的key */
        private final String cacheKey;
        /** grpc server */
        private final Server server;
        /** grpc 服务方法注册 */
        private final GrpcHandlerRegistry registry;
        /** server started标识 */
        private volatile boolean started;
        /** 引用数 */
        private final AtomicInteger referenceCount = new AtomicInteger(0);

        public GrpcServer(String cacheKey, Server server, GrpcHandlerRegistry registry) {
            this.cacheKey = cacheKey;
            this.server = server;
            this.registry = registry;
        }

        /**
         * start
         */
        public void start() {
            try {
                started = true;
                server.start();
            } catch (IOException e) {
                throw new RpcCallErrorException(e);
            }
        }

        /**
         * shutdown
         */
        public void close() {
            if (referenceCount.decrementAndGet() > 0) {
                return;
            }
            if (server.isShutdown() || server.isTerminated()) {
                return;
            }
            server.shutdown();
            SERVERS.invalidate(cacheKey);
        }

        /**
         * 引用数+1, 标识server可复用
         */
        public void incrementAndGetCount() {
            referenceCount.incrementAndGet();
        }
    }
}
