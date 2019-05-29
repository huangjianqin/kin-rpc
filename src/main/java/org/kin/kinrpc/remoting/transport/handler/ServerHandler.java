package org.kin.kinrpc.remoting.transport.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ServerHandler extends SimpleChannelInboundHandler<RPCRequest> {
    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);

    private BlockingQueue<RPCRequest> requestsQueue;

    public ServerHandler(BlockingQueue<RPCRequest> requestsQueue) {
        this.requestsQueue = requestsQueue;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCRequest rpcRequest) throws Exception {
        //简单地添加到任务队列交由上层的线程池去完成服务调用
        rpcRequest.setCtx(ctx);
        requestsQueue.put(rpcRequest);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("server throw exception:" + cause);
        ctx.close();
    }
}
