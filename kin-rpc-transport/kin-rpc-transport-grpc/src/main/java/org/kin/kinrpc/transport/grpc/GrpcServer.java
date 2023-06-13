package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import io.netty.buffer.ByteBuf;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.transport.AbsRemotingServer;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.grpc.interceptor.DefaultServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 仍然需要注册服务方法, 是因为需要充分利用http2 stream(一个服务方法对应一个stream), 实现server端多线程处理rpc请求
 * @author huangjianqin
 * @date 2023/6/8
 */
public class GrpcServer extends AbsRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    /** grpc server */
    private final Server server;
    /** grpc服务注册中心 */
    private final DefaultHandlerRegistry handlerRegistry = new DefaultHandlerRegistry();
    /** grpc service handler */
    private final ServerCallHandler<ByteBuf, ByteBuf> serviceHandler = ServerCalls.asyncUnaryCall(
            (byteBuf, streamObserver) -> remotingProcessor.process(new GrpcServerChannelContext(streamObserver), byteBuf));

    public GrpcServer(int port) {
        this(NetUtils.getLocalhostIp(), port);
    }

    public GrpcServer(String host, int port) {
        super(host, port);
        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forAddress(new InetSocketAddress(host, port));
        //user custom, user can not modify
        for (GrpcServerCustomizer customizer : ExtensionLoader.getExtensions(GrpcServerCustomizer.class)) {
            customizer.custom(serverBuilder);
        }
        //internal
        serverBuilder.intercept(DefaultServerInterceptor.INSTANCE)
                .fallbackHandlerRegistry(handlerRegistry);
        this.server = serverBuilder.build();
    }

    @Override
    public void start() {
        try {
            server.start();
            //注册message服务
            addService(GrpcMessages.SERVICE_NAME, GrpcMessages.METHOD_NAME);
            //注册generic服务
            addService(GrpcConstants.GENERIC_SERVICE_NAME, GrpcConstants.GENERIC_METHOD_NAME);
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
     * @param serviceName 服务名
     * @param methodNames 服务方法list
     */
    public void addService(String serviceName, String... methodNames) {
        addService(serviceName, Arrays.asList(methodNames));
    }

    /**
     * 注册服务及服务方法
     *
     * @param serviceName 服务名
     * @param methodNames 服务方法list
     */
    public void addService(String serviceName, List<String> methodNames) {
        ServerServiceDefinition.Builder serviceDefinitionBuilder = ServerServiceDefinition.builder(serviceName);
        for (String methodName : methodNames) {
            serviceDefinitionBuilder.addMethod(MethodDescriptor.<ByteBuf, ByteBuf>newBuilder()
                    // TODO: 2023/6/8 如果需要扩展stream request response, 则需要按方法返回值来设置method type
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(serviceName + "/" + methodName)
                    .setRequestMarshaller(ByteBufMarshaller.DEFAULT)
                    .setResponseMarshaller(ByteBufMarshaller.DEFAULT)
                    .build(), serviceHandler);
        }

        handlerRegistry.addService(serviceDefinitionBuilder.build());
    }

    /**
     * 注销服务及服务方法
     *
     * @param serviceName 服务名
     */
    public void removeService(String serviceName) {
        handlerRegistry.removeService(serviceName);
    }
}
