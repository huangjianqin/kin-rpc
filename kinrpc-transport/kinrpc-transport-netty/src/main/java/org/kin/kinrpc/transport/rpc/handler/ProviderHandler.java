package org.kin.kinrpc.transport.rpc.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.kin.kinrpc.transport.rpc.RPCRequestHandler;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.utils.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderHandler extends SimpleChannelInboundHandler<RPCRequest> {
    private static final Logger log = LoggerFactory.getLogger("transport");

    private RPCRequestHandler rpcRequestHandler;

    public ProviderHandler(RPCRequestHandler rpcRequestHandler) {
        this.rpcRequestHandler = rpcRequestHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCRequest rpcRequest) throws Exception {
        //简单地添加到任务队列交由上层的线程池去完成服务调用
        rpcRequest.setChannel(ctx.channel());
        if (rpcRequestHandler != null) {
            rpcRequestHandler.handleRequest(rpcRequest);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        log.error("server('{}') throw exception:{}", ChannelUtils.getIP(channel), cause);
        if (channel.isOpen() || channel.isActive()) {
            ctx.close();
        }
    }
}
