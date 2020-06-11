package org.kin.kinrpc.message.transport;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.RpcEndpointRefHandler;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.transport.netty.core.Client;
import org.kin.transport.netty.core.ClientTransportOption;
import org.kin.transport.netty.core.TransportOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public class TransportClient {
    private static final Logger log = LoggerFactory.getLogger(TransportClient.class);
    /** 序列化 */
    private RpcEnv rpcEnv;
    private RpcEndpointRefHandlerImpl rpcEndpointRefHandler;
    private volatile boolean isStopped;

    public TransportClient(RpcEnv rpcEnv) {
        this.rpcEnv = rpcEnv;
    }

    public void connect(String remoteHost, int remotePort, boolean compression) {
        if (isStopped) {
            return;
        }
        rpcEndpointRefHandler = new RpcEndpointRefHandlerImpl();

        InetSocketAddress address = new InetSocketAddress(remoteHost, remotePort);
        ClientTransportOption clientTransportOption =
                TransportOption.client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        //receive窗口缓存6mb
                        .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        //send窗口缓存64kb
                        .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                        .transportHandler(rpcEndpointRefHandler);

        if (compression) {
            clientTransportOption.compress();
        }

        rpcEndpointRefHandler.connect(clientTransportOption, address);
    }

    /**
     * @return 是否有效
     */
    public boolean isActive() {
        return isStopped && rpcEndpointRefHandler.isActive();
    }

    public void stop() {
        isStopped = true;
        rpcEndpointRefHandler.close();
    }

    /**
     * 发送消息
     */
    public void send(RpcMessage message) {
        if (isActive()) {
            //TODO
            byte[] data = rpcEnv.serialize(message);
            if (Objects.isNull(data)) {
                //TODO 序列化错误
                return;
            }

            RpcRequestProtocol protocol = RpcRequestProtocol.create(data);
            rpcEndpointRefHandler.client().request(protocol, new ReferenceRequestListener(message.getRequestId()));
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointRefHandlerImpl extends RpcEndpointRefHandler {
        @Override
        protected void handleRpcResponseProtocol(RpcResponseProtocol responseProtocol) {
            if (!TransportClient.this.isActive()) {
                return;
            }
            //反序列化内容
            byte[] data = responseProtocol.getRespContent();
            RpcMessage message = rpcEnv.deserialize(data);
            if (Objects.isNull(message)) {
                return;
            }

            //TODO
        }

        @Override
        protected void reconnect() {
            if (!isStopped) {
                //TODO
            }
        }

        public Client client() {
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
                //TODO
            }
        }
    }
}
