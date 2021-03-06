package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.kin.transport.netty.Server;
import org.kin.transport.netty.socket.SocketProtocolHandler;
import org.kin.transport.netty.socket.SocketTransportOption;
import org.kin.transport.netty.socket.protocol.SocketProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class KinRpcEndpointHandler extends SocketProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(KinRpcEndpointHandler.class);
    /** 服务器引用 */
    protected Server server;

    /**
     * 绑定端口并启动服务器
     */
    public final void bind(SocketTransportOption transportOption, InetSocketAddress address) {
        if (server != null) {
            server.close();
        }
        server = transportOption.bind(address);
    }

    /**
     * 关闭服务器
     */
    public final void close() {
        if (server != null) {
            server.close();
        }
    }

    /**
     * 服务器是否有效
     */
    public final boolean isActive() {
        return server != null && server.isActive();
    }

    /**
     * 处理请求协议
     */
    @Override
    public void handle(ChannelHandlerContext ctx, SocketProtocol protocol) {
        Channel channel = ctx.channel();
        if (protocol == null) {
            return;
        }
        if (protocol instanceof KinRpcRequestProtocol) {
            KinRpcRequestProtocol requestProtocol = (KinRpcRequestProtocol) protocol;
            handleRpcRequestProtocol(channel, requestProtocol);
        } else {
            //未知协议, 异常直接关闭channel
            channel.close();
            log.error("client({}) send unknown protocol >>>> {}", channel.remoteAddress(), protocol);
        }
    }

    /**
     * 处理Rpc请求协议
     */
    protected abstract void handleRpcRequestProtocol(Channel channel, KinRpcRequestProtocol requestProtocol);
}
