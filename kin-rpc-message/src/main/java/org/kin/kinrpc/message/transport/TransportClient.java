package org.kin.kinrpc.message.transport;

import io.netty.channel.ChannelHandlerContext;
import org.kin.framework.concurrent.HashedWheelTimer;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.OutBoxMessage;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcResponseCallback;
import org.kin.kinrpc.message.exception.AskMessageTimeoutException;
import org.kin.kinrpc.message.exception.ClientConnectFailException;
import org.kin.kinrpc.message.exception.ClientStoppedException;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.rpc.common.SslConfig;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.Serializations;
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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class TransportClient {
    private static final Logger log = LoggerFactory.getLogger(TransportClient.class);

    /** 序列化 */
    private final RpcEnv rpcEnv;
    /** 客户端配置 */
    private final SocketTransportOption clientTransportOption;
    /** 服务器地址 */
    private final KinRpcAddress rpcAddress;
    /** client handler */
    private final RpcEndpointRefHandlerImpl rpcEndpointRefHandler;
    private volatile boolean isStopped;
    /** 请求返回回调 */
    @SuppressWarnings("rawtypes")
    private final Map<Long, RpcResponseCallback> respCallbacks = new ConcurrentHashMap<>();
    /** 相当于OutBox发送消息逻辑, 用于重连时, 触发发送OutBox中仍然没有发送的消息 */
    private volatile Runnable connectionInitCallback;
    /** 超时计时器 */
    private HashedWheelTimer timer = new HashedWheelTimer(new SimpleThreadFactory("transportClient-send-timeout", true), 1, TimeUnit.MILLISECONDS, 2048);

    public TransportClient(RpcEnv rpcEnv, KinRpcAddress rpcAddress, CompressionType compressionType) {
        this.rpcEnv = rpcEnv;
        this.rpcEndpointRefHandler = new RpcEndpointRefHandlerImpl();
        this.rpcAddress = rpcAddress;

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
    public void connect() {
        if (isStopped) {
            return;
        }

        rpcEndpointRefHandler.connect(clientTransportOption, new InetSocketAddress(rpcAddress.getHost(), rpcAddress.getPort()), false);
    }

    /**
     * @return 是否有效
     */
    public boolean isActive() {
        return !isStopped && rpcEndpointRefHandler.isActive();
    }

    @SuppressWarnings("rawtypes")
    public void stop() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        rpcEndpointRefHandler.close();
        for (RpcResponseCallback callback : respCallbacks.values()) {
            callback.onFail(new ClientStoppedException(rpcAddress.address()));
        }
    }

    /**
     * 发送消息
     */
    @SuppressWarnings("rawtypes")
    public void send(OutBoxMessage outBoxMessage) {
        if (isActive()) {
            RpcMessage message = outBoxMessage.getMessage();
            byte[] data = rpcEnv.serialize(message);
            if (Objects.isNull(data)) {
                return;
            }

            long requestId = message.getRequestId();
            KinRpcRequestProtocol protocol = KinRpcRequestProtocol.create(requestId, (byte) rpcEnv.serialization().type(), data);
            if (rpcEndpointRefHandler.client().sendAndFlush(protocol)) {
                respCallbacks.put(requestId, outBoxMessage);
                long timeoutMs = outBoxMessage.getTimeoutMs();
                if (timeoutMs > 0) {
                    timer.newTimeout(to -> {
                        removeInvalidRespCallback(requestId);
                        RpcResponseCallback callback = respCallbacks.remove(message.getRequestId());
                        if (Objects.nonNull(callback)) {
                            callback.onFail(new AskMessageTimeoutException(outBoxMessage));
                        }
                    }, timeoutMs, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * 移除无效request绑定的callback
     */
    public void removeInvalidRespCallback(long requestId) {
        respCallbacks.remove(requestId);
    }

    /**
     * 更新client 连接成功建立callback
     */
    public void updateConnectionInitCallback(Runnable connectionInitCallback) {
        this.connectionInitCallback = connectionInitCallback;
    }

    //------------------------------------------------------------------------------------------------------------------
    @SuppressWarnings("rawtypes")
    private class RpcEndpointRefHandlerImpl extends KinRpcEndpointRefHandler {
        @SuppressWarnings("unchecked")
        @Override
        protected void handleRpcResponseProtocol(KinRpcResponseProtocol responseProtocol) {
            byte serializationType = responseProtocol.getSerialization();
            Serialization serialization = Serializations.getSerialization(serializationType);
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
                RpcResponseCallback callback = respCallbacks.remove(message.getRequestId());
                if (Objects.nonNull(callback)) {
                    callback.onSuccess(message.getMessage());
                }
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
            for (RpcResponseCallback callback : respCallbacks.values()) {
                callback.onFail(new ClientConnectFailException(rpcAddress.address()));
            }
        }

        public Client<SocketProtocol> client() {
            return client;
        }
    }
}
