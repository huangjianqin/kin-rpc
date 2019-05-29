package org.kin.kinrpc.remoting.transport.handler.consumer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.rpc.protocol.RPCResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceHandler extends SimpleChannelInboundHandler<RPCResponse> {
    private static final Logger log = Logger.getLogger(ReferenceHandler.class);

    private Map<String, RPCFuture> pendRPCFutureMap = new ConcurrentHashMap<String, RPCFuture>();
    private volatile Channel channel;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse response) throws Exception {
        log.info("收到一个响应");
        String requestId = response.getRequestId() + "";
        RPCFuture pendRPCFuture = pendRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendRPCFutureMap.remove(requestId);
            pendRPCFuture.done(response);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        this.pendRPCFutureMap.clear();
    }

    public RPCFuture request(RPCRequest request) {
        log.info("发送请求>>>" + request.toString());
        RPCFuture future = new RPCFuture(request);
        pendRPCFutureMap.put(request.getRequestId() + "", future);
        this.channel.writeAndFlush(request);

        return future;
    }
}
