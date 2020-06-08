package org.kin.kinrpc.transport;

import io.netty.channel.Channel;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.transport.netty.core.Server;
import org.kin.transport.netty.core.ServerTransportOption;
import org.kin.transport.netty.core.TransportHandler;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class RpcEndpointHandler extends TransportHandler {
    private static final Logger log = LoggerFactory.getLogger(RpcEndpointHandler.class);
    /** 服务器引用 */
    protected Server server;

    /**
     * 绑定端口并启动服务器
     */
    public final void bind(ServerTransportOption transportOption, InetSocketAddress address) throws Exception {
        if (server != null) {
            server.close();
        }
        server = transportOption.tcp(address);
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
    public final void handleProtocol(Channel channel, AbstractProtocol protocol) {
        if (protocol == null) {
            return;
        }
        if (protocol instanceof RpcRequestProtocol) {
            RpcRequestProtocol requestProtocol = (RpcRequestProtocol) protocol;
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
    protected abstract void handleRpcRequestProtocol(Channel channel, RpcRequestProtocol requestProtocol);
}
