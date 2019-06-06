package org.kin.kinrpc.transport.protocol.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.kin.kinrpc.transport.listener.ReadIdleListener;
import org.kin.kinrpc.transport.listener.WriteIdleListener;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ProtocolIdleTimeoutHandler extends ChannelDuplexHandler {
    private ReadIdleListener readIdleListener;
    private WriteIdleListener writeIdleListener;

    public ProtocolIdleTimeoutHandler(ReadIdleListener readIdleListener, WriteIdleListener writeIdleListener) {
        this.readIdleListener = readIdleListener;
        this.writeIdleListener = writeIdleListener;
    }

    public ProtocolIdleTimeoutHandler() {
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (readIdleListener != null) {
                    readIdleListener.readIdle(ctx.channel());
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                if (writeIdleListener != null) {
                    writeIdleListener.writeIdel(ctx.channel());
                }
            } else {
                //All IDLE
                if (readIdleListener != null) {
                    readIdleListener.readIdle(ctx.channel());
                }
                if (writeIdleListener != null) {
                    writeIdleListener.writeIdel(ctx.channel());
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
