package org.kin.kinrpc.remoting.transport.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/1/17.
 */
public final class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);
    private static final ByteBuf HEARTBEAT = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.UTF_8));

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //空闲一段时间触发这个事件
        if (evt instanceof IdleStateEvent) {
            //测试是否仍然与服务连接
            ctx.writeAndFlush(HEARTBEAT.duplicate()).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    //发送心跳失败,打印日志,关闭该连接,并
                    if (!channelFuture.isSuccess()) {
                        log.info("send heartbeat fail!!!");
                        log.info("close the service connection >>>" + channelFuture.channel().remoteAddress().toString());
                        channelFuture.channel().close();
                    }
                }
            });
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
