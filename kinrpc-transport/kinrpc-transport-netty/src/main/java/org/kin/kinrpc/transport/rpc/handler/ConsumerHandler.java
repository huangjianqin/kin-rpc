package org.kin.kinrpc.transport.rpc.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.kin.kinrpc.future.RPCFuture;
import org.kin.kinrpc.transport.Connection;
import org.kin.kinrpc.transport.rpc.RPCConstants;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ConsumerHandler extends SimpleChannelInboundHandler<RPCResponse> {
    private static final Logger log = LoggerFactory.getLogger("transport");

    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<String, RPCFuture>();
    private volatile Channel channel;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse response) throws Exception {
        log.info("收到一个响应");
        String requestId = response.getRequestId() + "";
        RPCFuture pendRPCFuture = pendingRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendingRPCFutureMap.remove(requestId);
            pendRPCFuture.done(response);
        }
    }

    private void clean() {
        for (RPCFuture rpcFuture : this.pendingRPCFutureMap.values()) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
            rpcResponse.setState(RPCResponse.State.ERROR, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();
        Connection connection = channel.attr(RPCConstants.CONNECTION_KEY).get();
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        clean();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        clean();
    }

    public Future<RPCResponse> request(RPCRequest request) {
        log.info("发送请求>>>" + request.toString());
        RPCFuture future = new RPCFuture(request);
        pendingRPCFutureMap.put(request.getRequestId() + "", future);
        this.channel.writeAndFlush(request);

        return future;
    }


}
