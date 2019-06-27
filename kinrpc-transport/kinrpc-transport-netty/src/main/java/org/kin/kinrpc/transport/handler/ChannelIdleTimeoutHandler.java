package org.kin.kinrpc.transport.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.kin.kinrpc.transport.listener.ChannelReadIdleListener;
import org.kin.kinrpc.transport.listener.ChannelWriteIdleListener;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ChannelIdleTimeoutHandler extends ChannelDuplexHandler {
    private ChannelReadIdleListener channelReadIdleListener;
    private ChannelWriteIdleListener channelWriteIdleListener;

    public ChannelIdleTimeoutHandler(ChannelReadIdleListener channelReadIdleListener, ChannelWriteIdleListener channelWriteIdleListener) {
        this.channelReadIdleListener = channelReadIdleListener;
        this.channelWriteIdleListener = channelWriteIdleListener;
    }

    public ChannelIdleTimeoutHandler() {
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (channelReadIdleListener != null) {
                    channelReadIdleListener.readIdle(ctx.channel());
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                if (channelWriteIdleListener != null) {
                    channelWriteIdleListener.writeIdel(ctx.channel());
                }
            } else {
                //All IDLE
                if (channelReadIdleListener != null) {
                    channelReadIdleListener.readIdle(ctx.channel());
                }
                if (channelWriteIdleListener != null) {
                    channelWriteIdleListener.writeIdel(ctx.channel());
                }
            }
        }
    }
}
