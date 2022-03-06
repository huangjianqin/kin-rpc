package org.kin.kinrpc.message.core;

import io.netty.channel.ChannelHandlerContext;
import org.kin.framework.concurrent.HashedWheelTimer;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.exception.ClientConnectFailException;
import org.kin.kinrpc.message.core.exception.ClientStoppedException;
import org.kin.kinrpc.message.core.exception.RequestResponseTimeoutException;
import org.kin.kinrpc.rpc.common.SslConfig;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.UnknownSerializationException;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.kinrpc.transport.kinrpc.KinRpcEndpointRefHandler;
import org.kin.kinrpc.transport.kinrpc.KinRpcRequestProtocol;
import org.kin.kinrpc.transport.kinrpc.KinRpcResponseProtocol;
import org.kin.transport.netty.Client;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.SocketTransportOption;
import org.kin.transport.netty.socket.protocol.SocketProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
final class TransportClient {
    private static final Logger log = LoggerFactory.getLogger(TransportClient.class);

    /** 序列化 */
    private final RpcEnv rpcEnv;
    /** 客户端配置 */
    private final SocketTransportOption clientTransportOption;
    /** 服务器地址 */
    private final KinRpcAddress remoteAddress;
    /** client handler */
    private final RpcEndpointRefHandlerImpl rpcEndpointRefHandler;
    private volatile boolean isStopped;
    /** 请求返回回调 */
    private final Map<Long, OutBoxMessage> requestId2OutBoxMessage = new ConcurrentHashMap<>();
    /** 相当于OutBox发送消息逻辑, 用于重连时, 触发发送OutBox中仍然没有发送的消息 */
    private volatile Runnable connectionInitCallback;
    /** 超时计时器 */
    private final HashedWheelTimer timer = new HashedWheelTimer(
            new SimpleThreadFactory("transportClient-send-timeout", true), 1, TimeUnit.MILLISECONDS, 2048);

    TransportClient(RpcEnv rpcEnv, KinRpcAddress remoteAddress, CompressionType compressionType) {
        this.rpcEnv = rpcEnv;
        this.rpcEndpointRefHandler = new RpcEndpointRefHandlerImpl();
        this.remoteAddress = remoteAddress;

        SocketTransportOption.SocketClientTransportOptionBuilder builder = Transports.socket().client()
                .channelOptions(rpcEnv.getClientChannelOptions())
                .protocolHandler(rpcEndpointRefHandler)
                .compress(compressionType);

        String certPath = SslConfig.INSTANCE.getClientKeyCertChainPath();
        String keyPath = SslConfig.INSTANCE.getClientPrivateKeyPath();

        if (StringUtils.isNotBlank(certPath) && StringUtils.isNotBlank(keyPath)) {
            builder.ssl(certPath, keyPath);
        }

        this.clientTransportOption = builder.build();
    }

    /**
     * 连接remote
     */
    void connect() {
        if (isStopped) {
            return;
        }

        rpcEndpointRefHandler.connect(clientTransportOption, new InetSocketAddress(remoteAddress.getHost(), remoteAddress.getPort()), false);
    }

    /**
     * @return 是否有效
     */
    boolean isActive() {
        return !isStopped && rpcEndpointRefHandler.isActive();
    }

    void stop() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        rpcEndpointRefHandler.close();
        for (OutBoxMessage outBoxMessage : requestId2OutBoxMessage.values()) {
            handleOutBoxMessageWhenException(outBoxMessage, new ClientStoppedException(remoteAddress.schema()));
        }
        //此处不需要让rpcEnv移除client
    }

    /**
     * 发送消息
     */
    void send(OutBoxMessage outBoxMessage) {
        if (isActive()) {
            RpcMessage message = outBoxMessage.getRpcMessage();
            byte[] data = rpcEnv.serialize(message);
            if (Objects.isNull(data)) {
                return;
            }

            long requestId = message.getRequestId();
            KinRpcRequestProtocol protocol = KinRpcRequestProtocol.create(requestId, (byte) rpcEnv.serialization().type(), data);
            if (rpcEndpointRefHandler.client().sendAndFlush(protocol)) {
                requestId2OutBoxMessage.put(requestId, outBoxMessage);
                long timeoutMs = outBoxMessage.getTimeoutMs();
                if (timeoutMs > 0) {
                    timer.newTimeout(to -> {
                        removeInvalidWaitingResponseMessage(requestId);
                        handleOutBoxMessageWhenException(requestId2OutBoxMessage.remove(message.getRequestId()), new RequestResponseTimeoutException(outBoxMessage.getRpcMessage().getMessage()));
                    }, timeoutMs, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * 移除无效等待返回的request
     */
    void removeInvalidWaitingResponseMessage(long requestId) {
        requestId2OutBoxMessage.remove(requestId);
    }

    /**
     * 更新client 连接成功建立callback
     */
    void updateConnectionInitCallback(Runnable connectionInitCallback) {
        this.connectionInitCallback = connectionInitCallback;
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointRefHandlerImpl extends KinRpcEndpointRefHandler {
        @Override
        protected void handleRpcResponseProtocol(KinRpcResponseProtocol responseProtocol) {
            byte serializationType = responseProtocol.getSerialization();
            Serialization serialization = ExtensionLoader.getExtension(Serialization.class, serializationType);
            if (Objects.isNull(serialization)) {
                throw new UnknownSerializationException(serializationType);
            }

            //反序列化内容
            byte[] data = responseProtocol.getRespContent();
            //处理receiver返回的消息
            if (!TransportClient.this.isActive()) {
                return;
            }
            RpcMessage message = rpcEnv.deserialize(serialization, data);
            if (Objects.isNull(message)) {
                return;
            }

            //callback回调
            String targetReceiver = message.getTo().getEndpointAddress().getName();
            if (StringUtils.isBlank(targetReceiver)) {
                handleOutBoxMessageWhenResponse(requestId2OutBoxMessage.remove(message.getRequestId()), message.getMessage());
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if (Objects.nonNull(connectionInitCallback)) {
                connectionInitCallback.run();
            }
        }

        @Override
        protected void connectionInactive() {
            for (OutBoxMessage outBoxMessage : requestId2OutBoxMessage.values()) {
                handleOutBoxMessageWhenException(outBoxMessage, new ClientConnectFailException(remoteAddress.schema()));
            }
            rpcEnv.removeClient(remoteAddress);
            //触发outbox drain, 如果outbox还有消息未发送则需要不断尝试重连然后把剩余的消息push出去
            if (Objects.nonNull(connectionInitCallback)) {
                connectionInitCallback.run();
            }
        }

        Client<SocketProtocol> client() {
            return client;
        }
    }

    /**
     * 选择合适的executor触发{@link RpcCallback#onResponse(long, Serializable, Serializable)}
     */
    @SuppressWarnings("unchecked")
    private void handleOutBoxMessageWhenResponse(OutBoxMessage outBoxMessage, Serializable response) {
        if (Objects.isNull(outBoxMessage)) {
            return;
        }
        if (outBoxMessage.isExecCallback()) {
            //callback
            RpcCallback callback = outBoxMessage.getCallback();
            RpcMessage rpcMessage = outBoxMessage.getRpcMessage();
            RpcCallback.executor(callback, rpcEnv).execute(() -> {
                callback.onResponse(rpcMessage.getRequestId(), rpcMessage.getMessage(), response);
            });
        }
        if (outBoxMessage.isCompleteFuture()) {
            //future
            outBoxMessage.getSource().complete(response);
        }
    }

    /**
     * 选择合适的executor触发{@link RpcCallback#onException(Throwable)}
     */
    private void handleOutBoxMessageWhenException(OutBoxMessage outBoxMessage, Throwable e) {
        if (Objects.isNull(outBoxMessage)) {
            return;
        }
        if (outBoxMessage.isExecCallback()) {
            //callback
            RpcCallback callback = outBoxMessage.getCallback();
            RpcCallback.executor(callback, rpcEnv).execute(() -> callback.onException(e));
        }
        if (outBoxMessage.isCompleteFuture()) {
            //future
            outBoxMessage.getSource().completeExceptionally(e);
        }
    }
}
