package org.kin.kinrpc.message.transport;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.OutBoxMessage;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcResponseCallback;
import org.kin.kinrpc.message.exception.ClientConnectFailException;
import org.kin.kinrpc.message.exception.ClientStoppedException;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.RpcEndpointRefHandler;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.kinrpc.transport.serializer.UnknownSerializerException;
import org.kin.transport.netty.Client;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.client.SocketClientTransportOption;
import org.kin.transport.netty.socket.protocol.SocketProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public class TransportClient {
    private static final Logger log = LoggerFactory.getLogger(TransportClient.class);
    /** 序列化 */
    private RpcEnv rpcEnv;
    /** 客户端配置 */
    private SocketClientTransportOption clientTransportOption;
    /** 服务器地址 */
    private RpcAddress rpcAddress;
    /** client handler */
    private RpcEndpointRefHandlerImpl rpcEndpointRefHandler;
    private volatile boolean isStopped;
    /** 请求返回回调 */
    private Map<Long, RpcResponseCallback> respCallbacks = new ConcurrentHashMap<>();

    public TransportClient(RpcEnv rpcEnv, RpcAddress rpcAddress, boolean compression) {
        this.rpcEnv = rpcEnv;
        this.rpcEndpointRefHandler = new RpcEndpointRefHandlerImpl();
        this.rpcAddress = rpcAddress;
        this.clientTransportOption =
                Transports.socket().client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.SO_KEEPALIVE, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        //receive窗口缓存6mb
                        .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        //send窗口缓存64kb
                        .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                        .protocolHandler(rpcEndpointRefHandler);

        if (compression) {
            this.clientTransportOption.compress();
        }
    }

    /**
     * 连接remote
     */
    public void connect() {
        if (isStopped) {
            return;
        }

        rpcEndpointRefHandler.connect(clientTransportOption, new InetSocketAddress(rpcAddress.getHost(), rpcAddress.getPort()));
    }

    /**
     * @return 是否有效
     */
    public boolean isActive() {
        return !isStopped && rpcEndpointRefHandler.isActive();
    }

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
    public void send(OutBoxMessage outBoxMessage) {
        if (isActive()) {
            RpcMessage message = outBoxMessage.getMessage();
            byte[] data = rpcEnv.serialize(message);
            if (Objects.isNull(data)) {
                return;
            }

            long requestId = message.getRequestId();
            RpcRequestProtocol protocol = RpcRequestProtocol.create(requestId, (byte) rpcEnv.serializer().type(), data);
            respCallbacks.put(requestId, outBoxMessage);
            rpcEndpointRefHandler.client().request(protocol, new ReferenceRequestListener(requestId));
        }
    }

    /**
     * 移除无效request绑定的callback
     */
    public void removeRpcMessage(long requestId) {
        respCallbacks.remove(requestId);
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointRefHandlerImpl extends RpcEndpointRefHandler {
        @Override
        protected void handleRpcResponseProtocol(RpcResponseProtocol responseProtocol) {
            byte serializerType = responseProtocol.getSerializer();
            Serializer serializer = Serializers.getSerializer(serializerType);
            if (Objects.isNull(serializer)) {
                throw new UnknownSerializerException(serializerType);
            }

            //反序列化内容
            byte[] data = responseProtocol.getRespContent();
            //处理receiver返回的消息
            if (!TransportClient.this.isActive()) {
                return;
            }
            RpcMessage message = rpcEnv.deserialize(serializer, data);
            if (Objects.isNull(message)) {
                return;
            }

            //callback回调
            String targetReceiver = message.getTo().getEndpointAddress().getName();
            if (StringUtils.isBlank(targetReceiver)) {
                RpcResponseCallback callback = respCallbacks.get(message.getRequestId());
                if (Objects.nonNull(callback)) {
                    callback.onSuccess(message.getMessage());
                }
            }
        }

        @Override
        protected void reconnect() {
            //不处理, 由outbox发现in active时, 重新连接
            rpcEnv.removeClient(rpcAddress);
            for (RpcResponseCallback callback : respCallbacks.values()) {
                callback.onFail(new ClientConnectFailException(rpcAddress.address()));
            }
        }

        public Client<SocketProtocol> client() {
            return client;
        }
    }

    private class ReferenceRequestListener implements ChannelFutureListener {
        private long requestId;

        public ReferenceRequestListener(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
                //发送消息时遇到异常
                removeRpcMessage(requestId);
            }
        }
    }
}
