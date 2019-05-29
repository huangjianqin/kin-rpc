package org.kin.kinrpc.remoting.transport.handler.provider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;
import org.kin.kinrpc.rpc.protocol.RPCRequest;

import java.util.concurrent.BlockingQueue;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderHandler extends SimpleChannelInboundHandler<RPCRequest> {
    private static final Logger log = Logger.getLogger(ProviderHandler.class);

    private BlockingQueue<RPCRequest> requestsQueue;

    public ProviderHandler(BlockingQueue<RPCRequest> requestsQueue) {
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
