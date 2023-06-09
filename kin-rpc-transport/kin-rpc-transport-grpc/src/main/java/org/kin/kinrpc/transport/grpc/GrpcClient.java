package org.kin.kinrpc.transport.grpc;

import io.grpc.*;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.buffer.ByteBuf;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.transport.AbsRemotingClient;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class GrpcClient extends AbsRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcClient.class);
    /** grpc client channel */
    private volatile ManagedChannel channel;
    /** grpc client call method descriptor cache */
    private final Map<String, MethodDescriptor<ByteBuf, ByteBuf>> methodDescriptorCache = new CopyOnWriteMap<>();

    public GrpcClient(int port) {
        this(NetUtils.getLocalhostIp(), port);
    }

    public GrpcClient(String host, int port) {
        super(host, port);
    }

    @Override
    public void start() {
        if (Objects.nonNull(channel)) {
            throw new IllegalStateException(String.format("grpc client has been connect to %s:%d", host, port));
        }

        this.channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        log.info("grpc client connect to {}:{} success", host, port);
    }

    @Override
    public void shutdown() {
        checkStarted();

        channel.shutdown();
        remotingProcessor.shutdown();
        log.info("grpc client(- R:{}:{}) terminated", host, port);
    }

    /**
     * 检查client是否started
     */
    private void checkStarted() {
        if (Objects.isNull(channel)) {
            throw new IllegalStateException(String.format("grpc client does not start to connect to %s:%d", host, port));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        if (Objects.isNull(command)) {
            throw new IllegalArgumentException("request command is null");
        }

        checkStarted();

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        if (command instanceof RpcRequestCommand) {
            return (CompletableFuture<T>) rpcCall((RpcRequestCommand) command, requestFuture);
        } else if (command instanceof MessageCommand) {
            return (CompletableFuture<T>) messageCall((MessageCommand) command, requestFuture);
        }
        throw new UnsupportedOperationException(String.format("does not support request command '%s' now", command.getClass()));
    }

    /**
     * rpc call
     *
     * @param command rpc request command
     */
    private CompletableFuture<Object> rpcCall(@Nonnull RpcRequestCommand command,
                                              CompletableFuture<Object> requestFuture) {
        String gsv = command.getGsv();
        String method = command.getMethod();
        String cacheKey = getMethodDescriptorCacheKey(gsv, method);
        MethodDescriptor<ByteBuf, ByteBuf> methodDescriptor = methodDescriptorCache.get(cacheKey);
        if (Objects.isNull(methodDescriptor)) {
            //fallback
            log.warn("does not find method descriptor fallback to generic method descriptor, service name = {}, method name = {}", gsv, method);
            methodDescriptor = GrpcConstants.GENERIC_METHOD_DESCRIPTOR;
        }

        return call(methodDescriptor, command, codec.encode(command), requestFuture);
    }

    /**
     * message call
     *
     * @param command message command
     */
    private CompletableFuture<Object> messageCall(@Nonnull MessageCommand command,
                                                  CompletableFuture<Object> requestFuture) {
        return call(GrpcMessages.METHOD_DESCRIPTOR, command, codec.encode(command), requestFuture);
    }

    /**
     * grpc call
     *
     * @param methodDescriptor grpc service method descriptor
     * @param payload          call payload
     */
    private CompletableFuture<Object> call(MethodDescriptor<ByteBuf, ByteBuf> methodDescriptor,
                                           RequestCommand command,
                                           ByteBuf payload,
                                           CompletableFuture<Object> requestFuture) {
        ClientCall<ByteBuf, ByteBuf> clientCall = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
        // TODO: 2023/6/8 是否需要设置header
        clientCall.start(new ClientCall.Listener<ByteBuf>() {
            @Override
            public void onMessage(ByteBuf byteBuf) {
                remotingProcessor.process(clientChannelContext, byteBuf);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (!status.isOk()) {
                    removeRequestFuture(command.getId());
                    requestFuture.completeExceptionally(status.getCause());
                }
            }
        }, new Metadata());

        clientCall.sendMessage(payload);
        clientCall.halfClose();
        clientCall.request(1);

        return requestFuture;
    }

    /**
     * 获取method descriptor cache key
     *
     * @param serviceName 服务名
     * @param method      服务方法名
     * @return method descriptor cache key
     */
    private String getMethodDescriptorCacheKey(String serviceName, String method) {
        return serviceName + "." + method;
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
        for (String methodName : methodNames) {
            String fullName = serviceName + "/" + methodName;
            MethodDescriptor<ByteBuf, ByteBuf> methodDescriptor = MethodDescriptor.<ByteBuf, ByteBuf>newBuilder()
                    // TODO: 2023/6/8 如果需要扩展stream request response, 则需要按方法返回值来设置method type
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(fullName)
                    .setRequestMarshaller(ByteBufMarshaller.DEFAULT)
                    .setResponseMarshaller(ByteBufMarshaller.DEFAULT)
                    .build();
            methodDescriptorCache.put(fullName, methodDescriptor);
        }
    }
}
