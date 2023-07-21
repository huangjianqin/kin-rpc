package org.kin.kinrpc.transport.grpc;

import io.grpc.Server;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import io.netty.buffer.ByteBuf;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.AbstractRemotingServer;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.grpc.interceptor.DefaultServerInterceptor;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.HandlerUtils;
import org.kin.transport.netty.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 仍然需要注册服务方法, 是因为需要充分利用http2 stream(一个服务方法对应一个stream), 实现server端多线程处理rpc请求
 *
 * @author huangjianqin
 * @date 2023/6/8
 */
public class GrpcServer extends AbstractRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    /** grpc server */
    private final Server server;
    /** grpc服务注册中心 */
    private final DefaultHandlerRegistry handlerRegistry = new DefaultHandlerRegistry();
    /** grpc service handler */
    private final ServerCallHandler<ByteBuf, ByteBuf> serviceHandler = ServerCalls.asyncUnaryCall(
            (byteBuf, streamObserver) -> remotingProcessor.process(new GrpcServerChannelContext(streamObserver), byteBuf));

    public GrpcServer(int port) {
        this(port, null);
    }

    public GrpcServer(int port, SslConfig sslConfig) {
        this(port, null, sslConfig);
    }

    public GrpcServer(int port,
                      ManagedExecutor executor,
                      SslConfig sslConfig) {
        this(NetUtils.getLocalhost4Ip(), port, executor, sslConfig);
    }

    public GrpcServer(String host, int port,
                      @Nullable ManagedExecutor executor,
                      @Nullable SslConfig sslConfig) {
        super(host, port, executor);
        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forAddress(new InetSocketAddress(host, port));
        //user custom, user can not modify
        for (GrpcServerCustomizer customizer : ExtensionLoader.getExtensions(GrpcServerCustomizer.class)) {
            customizer.custom(serverBuilder);
        }
        //internal
        serverBuilder.intercept(DefaultServerInterceptor.INSTANCE)
                .fallbackHandlerRegistry(handlerRegistry);
        if (Objects.nonNull(sslConfig)) {
            //ssl
            serverBuilder.sslContext(SslUtils.getServerSslContext(
                    sslConfig.getCertFile(), sslConfig.getCertKeyFile(), sslConfig.getCertKeyPassword(),
                    sslConfig.getCaFile(), sslConfig.getFingerprintFile()));
        }

        this.server = serverBuilder.build();
    }

    @Override
    public void start() {
        try {
            server.start();
            //注册message服务
            registerService(GsvUtils.serviceId(GrpcMessages.SERVICE_NAME),
                    HandlerUtils.handlerId(GrpcMessages.SERVICE_NAME, GrpcMessages.METHOD_NAME));
            //注册generic服务
            registerService(GsvUtils.serviceId(GrpcConstants.GENERIC_SERVICE_NAME),
                    HandlerUtils.handlerId(GrpcConstants.GENERIC_SERVICE_NAME, GrpcConstants.GENERIC_METHOD_NAME));
            log.info("grpc server started on {}:{}", host, port);
        } catch (IOException e) {
            shutdown();
            throw new TransportException("grpc server start fail", e);
        }
    }

    @Override
    public void shutdown() {
        server.shutdown();
        remotingProcessor.shutdown();
        log.info("grpc server({}:{}) terminated", host, port);
    }

    /**
     * 注册服务及服务方法
     *
     * @param serviceId  服务唯一id
     * @param handlerIds 服务方法唯一id array
     */
    public void registerService(int serviceId, Integer... handlerIds) {
        registerService(serviceId, Arrays.asList(handlerIds));
    }

    /**
     * 注册服务及服务方法
     *
     * @param serviceId  服务唯一id
     * @param handlerIds 服务方法唯一id list
     */
    public void registerService(int serviceId, List<Integer> handlerIds) {
        ServerServiceDefinition.Builder serviceDefinitionBuilder = ServerServiceDefinition.builder(GrpcConstants.SERVICE_PREFIX + serviceId);
        for (Integer handlerId : handlerIds) {
            serviceDefinitionBuilder.addMethod(GrpcUtils.genMethodDescriptor(serviceId, handlerId), serviceHandler);
        }

        handlerRegistry.addService(serviceId, serviceDefinitionBuilder.build());
    }

    /**
     * 注销服务及服务方法
     *
     * @param serviceId 服务唯一id
     */
    public void unregisterService(int serviceId) {
        handlerRegistry.removeService(serviceId);
    }
}
