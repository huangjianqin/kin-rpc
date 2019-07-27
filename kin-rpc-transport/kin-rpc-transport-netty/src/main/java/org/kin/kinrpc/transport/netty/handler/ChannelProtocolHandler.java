package org.kin.kinrpc.transport.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.kin.kinrpc.transport.netty.AbstractSession;
import org.kin.kinrpc.transport.netty.ChannelExceptionHandler;
import org.kin.kinrpc.transport.netty.ProtocolHandler;
import org.kin.kinrpc.transport.netty.SessionBuilder;
import org.kin.kinrpc.transport.netty.common.ProtocolConstants;
import org.kin.kinrpc.transport.netty.listener.ChannelActiveListener;
import org.kin.kinrpc.transport.netty.listener.ChannelInactiveListener;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.netty.utils.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ChannelProtocolHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ChannelProtocolHandler.class);
    private final ProtocolHandler protocolHandler;
    private final SessionBuilder sessionBuilder;
    private ChannelActiveListener channelActiveListener;
    private ChannelInactiveListener channelInactiveListener;
    private ChannelExceptionHandler channelExceptionHandler;

    public ChannelProtocolHandler(ProtocolHandler protocolHandler,
                                  SessionBuilder sessionBuilder,
                                  ChannelActiveListener channelActiveListener,
                                  ChannelInactiveListener channelInactiveListener,
                                  ChannelExceptionHandler channelExceptionHandler) {
        this.protocolHandler = protocolHandler;
        this.sessionBuilder = sessionBuilder;
        this.channelActiveListener = channelActiveListener;
        this.channelInactiveListener = channelInactiveListener;
        this.channelExceptionHandler = channelExceptionHandler;
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
                                  ChannelExceptionHandler channelExceptionHandler) {
        this(protocolHandler, sessionBuilder, null, null, channelExceptionHandler);
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
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel inactive: {}", ctx.channel());
        if (channelInactiveListener != null) {
            try {
                channelInactiveListener.channelInactive(ctx.channel());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        log.error("server('{}') throw exception:{}", ChannelUtils.getIP(channel), cause);
        if (channel.isOpen() || channel.isActive()) {
            ctx.close();
        }
        if (channelExceptionHandler != null) {
            try {
                channelExceptionHandler.handleException(ctx.channel(), cause);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
