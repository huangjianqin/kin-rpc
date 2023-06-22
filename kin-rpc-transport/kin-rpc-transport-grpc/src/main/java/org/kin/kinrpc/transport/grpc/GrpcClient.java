package org.kin.kinrpc.transport.grpc;

import io.grpc.*;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.buffer.ByteBuf;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.transport.AbsRemotingClient;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
    protected void onConnect() {
        if (Objects.nonNull(channel)) {
            throw new IllegalStateException(String.format("%s has been connect to %s", name(), remoteAddress()));
        }

        onConnect0();
    }

    private void onConnect0() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port);
        //user custom
        for (GrpcClientCustomizer customizer : ExtensionLoader.getExtensions(GrpcClientCustomizer.class)) {
            customizer.custom(channelBuilder);
        }
        //internal, user can not modify
        channelBuilder.usePlaintext();
        //async connect
        this.channel = channelBuilder.build();
        checkChannelConnectState();
    }

    /**
     * 检查channel state
     */
    private void checkChannelConnectState() {
        ConnectivityState curState = this.channel.getState(true);
        if (ConnectivityState.READY.equals(curState)) {
            onConnectSuccess();
        } else if (ConnectivityState.TRANSIENT_FAILURE.equals(curState)) {
            onConnectFail(new TransportException(String.format("%s connect to %s fail", name(), remoteAddress())));
        } else if (ConnectivityState.SHUTDOWN.equals(curState)) {
            // TODO: 2023/6/21  还没想到如何处理SHUTDOWN state
            log.warn("doesn't handle state 'SHUTDOWN' when grpc client connect");
        } else {
            //connecting or idle state, 还没建立channel
            this.channel.notifyWhenStateChanged(curState, this::checkChannelConnectState);
        }
    }

    @Override
    protected void onShutdown() {
        if (Objects.isNull(channel)) {
            return;
        }

        channel.shutdown();
        remotingProcessor.shutdown();
        log.info("{} terminated", name());
    }

    @Override
    protected void onReconnect() {
        if (Objects.nonNull(channel) && !channel.isTerminated()) {
            channel.shutdown();
        }

        onConnect0();
    }

    /**
     * call之前的操作, 一般用于检查
     *
     * @param command request command
     */
    private void beforeCall(RemotingCommand command) {
        if (Objects.isNull(command)) {
            throw new IllegalArgumentException("request command is null");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        beforeCall(command);

        CompletableFuture<T> requestFuture = (CompletableFuture<T>) createRequestFuture(command.getId());
        return call(command, requestFuture);
    }

    @Override
    public void fireAndForget(@Nonnull RequestCommand command) {
        beforeCall(command);

        call(command, null);
    }

    @Override
    protected CompletableFuture<Void> heartbeat() {
        HeartbeatCommand command = new HeartbeatCommand();
        beforeCall(command);

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        call(command, requestFuture);

        return CompletableFuture.allOf(requestFuture);
    }

    /**
     * grpc call
     *
     * @param command       request command
     * @param requestFuture request future
     */
    private <T> CompletableFuture<T> call(RemotingCommand command,
                                          CompletableFuture<T> requestFuture) {
        if (command instanceof RpcRequestCommand) {
            return rpcCall((RpcRequestCommand) command, requestFuture);
        } else if (command instanceof MessageCommand) {
            return messageCall((MessageCommand) command, requestFuture);
        } else if (command instanceof HeartbeatCommand) {
            return heartbeatCall((HeartbeatCommand) command, requestFuture);
        }
        throw new UnsupportedOperationException(String.format("does not support request command '%s' now", command.getClass()));
    }

    /**
     * rpc call
     *
     * @param command rpc request command
     */
    private <T> CompletableFuture<T> rpcCall(@Nonnull RpcRequestCommand command,
                                             CompletableFuture<T> requestFuture) {
        String gsv = command.getGsv();
        String method = command.getMethod();
        String cacheKey = getMethodDescriptorCacheKey(gsv, method);
        MethodDescriptor<ByteBuf, ByteBuf> methodDescriptor = methodDescriptorCache.get(cacheKey);
        if (Objects.isNull(methodDescriptor)) {
            //fallback
            log.warn("does not find method descriptor fallback to generic method descriptor, service name = {}, method name = {}", gsv, method);
            methodDescriptor = GrpcConstants.GENERIC_METHOD_DESCRIPTOR;
        }

        return callNow(methodDescriptor, command, codec.encode(command), requestFuture);
    }

    /**
     * message call
     *
     * @param command message command
     */
    private <T> CompletableFuture<T> messageCall(@Nonnull MessageCommand command,
                                                 CompletableFuture<T> requestFuture) {
        return callNow(GrpcMessages.METHOD_DESCRIPTOR, command, codec.encode(command), requestFuture);
    }

    /**
     * heartbeat call
     *
     * @param command heartbeat command
     */
    private <T> CompletableFuture<T> heartbeatCall(@Nonnull HeartbeatCommand command,
                                                   CompletableFuture<T> requestFuture) {
        return callNow(GrpcConstants.GENERIC_METHOD_DESCRIPTOR, command, codec.encode(command), requestFuture);
    }

    /**
     * grpc call
     *
     * @param methodDescriptor grpc service method descriptor
     * @param command          request command
     * @param payload          call payload
     * @param requestFuture    request future
     */
    private <T> CompletableFuture<T> callNow(MethodDescriptor<ByteBuf, ByteBuf> methodDescriptor,
                                             RemotingCommand command,
                                             ByteBuf payload,
                                             CompletableFuture<T> requestFuture) {
        ClientCall<ByteBuf, ByteBuf> clientCall = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
        // TODO: 2023/6/8 是否需要设置header
        clientCall.start(new ClientCall.Listener<ByteBuf>() {
            @Override
            public void onMessage(ByteBuf byteBuf) {
                remotingProcessor.process(clientChannelContext, byteBuf);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (!status.isOk() && Objects.nonNull(requestFuture)) {
                    removeRequestFuture(command.getId());
                    requestFuture.completeExceptionally(status.getCause());

                    if (command instanceof RequestCommand) {
                        onRequestFail(status.getCause());
                    }
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
