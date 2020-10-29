package org.kin.kinrpc.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.transport.netty.Client;
import org.kin.transport.netty.socket.SocketProtocolHandler;
import org.kin.transport.netty.socket.client.SocketClientTransportOption;
import org.kin.transport.netty.socket.protocol.SocketProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class RpcEndpointRefHandler extends SocketProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(RpcEndpointRefHandler.class);

    /** 客户端引用 */
    protected volatile Client<SocketProtocol> client;
    /** 是否已关闭 */
    protected volatile boolean isStopped;

    /**
     * 连接服务器
     */
    public final void connect(SocketClientTransportOption transportOption, InetSocketAddress address) {
        if (isStopped) {
            return;
        }
        if (isActive()) {
            return;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (client == null) {
            try {
                client = transportOption.withReconnect(address, false);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 关闭客户端
     */
    public final void close() {
        isStopped = true;
        if (Objects.nonNull(client)) {
            client.close();
        }
    }

    /**
     * 客户端是否有效
     */
    public final boolean isActive() {
        return !isStopped && client != null && client.isActive();
    }

    /**
     * 处理返回协议
     */
    @Override
    public final void handle(ChannelHandlerContext ctx, SocketProtocol protocol) {
        Channel channel = ctx.channel();
        if (!isActive()) {
            return;
        }
        if (Objects.isNull(protocol)) {
            return;
        }
        if (protocol instanceof RpcResponseProtocol) {
            RpcResponseProtocol responseProtocol = (RpcResponseProtocol) protocol;
            handleRpcResponseProtocol(responseProtocol);
        } else {
            //未知协议, 异常直接关闭channel
            channel.close();
            log.error("server({}) send unknown protocol >>>> {}", channel.remoteAddress(), protocol);
        }
    }

    /**
     * 客户端断开处理
     */
    @Override
    public final void channelInactive(ChannelHandlerContext ctx) {
        connectionInactive();
    }

    /**
     * 处理返回协议
     */
    protected abstract void handleRpcResponseProtocol(RpcResponseProtocol responseProtocol);

    /**
     * 连接断开逻辑处理
     */
    protected abstract void connectionInactive();
}
