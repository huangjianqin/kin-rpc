package org.kin.kinrpc.transport.protocol.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.kin.kinrpc.transport.listener.ChannelActiveListener;
import org.kin.kinrpc.transport.listener.ChannelInactiveListener;
import org.kin.kinrpc.transport.listener.ExceptionHandler;
import org.kin.kinrpc.transport.protocol.AbstractSession;
import org.kin.kinrpc.transport.protocol.ProtocolConstants;
import org.kin.kinrpc.transport.protocol.ProtocolHandler;
import org.kin.kinrpc.transport.protocol.SessionBuilder;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;
import org.kin.kinrpc.transport.utils.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ChannelProtocolHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger("transport");
    private final ProtocolHandler protocolHandler;
    private final SessionBuilder sessionBuilder;
    private ChannelActiveListener channelActiveListener;
    private ChannelInactiveListener channelInactiveListener;
    private ExceptionHandler exceptionHandler;

    public ChannelProtocolHandler(ProtocolHandler protocolHandler,
                                  SessionBuilder sessionBuilder,
                                  ChannelActiveListener channelActiveListener,
                                  ChannelInactiveListener channelInactiveListener,
                                  ExceptionHandler exceptionHandler) {
        this.protocolHandler = protocolHandler;
        this.sessionBuilder = sessionBuilder;
        this.channelActiveListener = channelActiveListener;
        this.channelInactiveListener = channelInactiveListener;
        this.exceptionHandler = exceptionHandler;
    }

    public ChannelProtocolHandler(ProtocolHandler protocolHandler, SessionBuilder sessionBuilder) {
        this(protocolHandler, sessionBuilder, null, null, null);
    }

    public ChannelProtocolHandler(ProtocolHandler protocolHandler,
                                  SessionBuilder sessionBuilder,
                                  ChannelActiveListener channelActiveListener) {
        this(protocolHandler, sessionBuilder, channelActiveListener, null, null);
    }

    public ChannelProtocolHandler(ProtocolHandler protocolHandler,
                                  SessionBuilder sessionBuilder,
                                  ChannelInactiveListener channelInactiveListener) {
        this(protocolHandler, sessionBuilder, null, channelInactiveListener, null);
    }

    public ChannelProtocolHandler(ProtocolHandler protocolHandler,
                                  SessionBuilder sessionBuilder,
                                  ExceptionHandler exceptionHandler) {
        this(protocolHandler, sessionBuilder, null, null, exceptionHandler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractProtocol protocol = (AbstractProtocol) msg;

        log.debug("Recv {} {} {}", Arrays.asList(protocol.getProtocolId(), protocol.getClass().getSimpleName(), ctx.channel()));

        AbstractSession session = ProtocolConstants.session(ctx.channel());
        if (session != null) {
            protocolHandler.handleProtocol(session, protocol);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel active: {}", ctx.channel());
        Attribute<AbstractSession> attr = ctx.channel().attr(ProtocolConstants.SESSION_KEY);
        if (!attr.compareAndSet(null, sessionBuilder.create(ctx.channel()))) {
            ctx.channel().close();
            log.error("Duplicate Session! IP: {}", ChannelUtils.getIP(ctx.channel()));
            return;
        }
        if (channelActiveListener != null) {
            try {
                channelActiveListener.channelActive(ctx.channel());
            } catch (Exception e) {
                log.error("", e);
            }
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel inactive: {}", ctx.channel());
        if (channelInactiveListener != null) {
            try {
                channelInactiveListener.channelInactive(ctx.channel());
            } catch (Exception e) {
                log.error("", e);
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        log.error("server('{}') throw exception:{}", ChannelUtils.getIP(channel), cause);
        if (channel.isOpen() || channel.isActive()) {
            ctx.close();
        }
        if (exceptionHandler != null) {
            try {
                exceptionHandler.handleException(ctx.channel(), cause);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        super.exceptionCaught(ctx, cause);
    }
}
