package org.kin.kinrpc.transport.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.listener.*;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.handler.ChannelProtocolHandler;
import org.kin.kinrpc.transport.protocol.handler.ProtocolCodec;
import org.kin.kinrpc.transport.protocol.handler.ProtocolIdleTimeoutHandler;
import org.kin.kinrpc.transport.protocol.impl.DefaultSessionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public class Client extends AbstractConnection {
    private static final Logger log = LoggerFactory.getLogger("transport");

    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    //建立好连接后才可以进行一些操作
    private CountDownLatch latch = new CountDownLatch(1);
    //连接超时毫秒数
    private int timeout;

    private final Bytes2ProtocolTransfer transfer;
    private final ProtocolHandler protocolHandler;
    private SessionBuilder sessionBuilder = new DefaultSessionBuilder();
    private ChannelActiveListener channelActiveListener;
    private ChannelInactiveListener channelInactiveListener;
    private ExceptionHandler exceptionHandler;
    private ReadIdleListener readIdleListener;
    private WriteIdleListener writeIdleListener;

    public Client(InetSocketAddress address, Bytes2ProtocolTransfer transfer, ProtocolHandler protocolHandler) {
        super(address);
        this.transfer = transfer;
        this.protocolHandler = protocolHandler;
    }

    @Override
    public void connect() {
        log.info("client connecting...");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new WriteTimeoutHandler(10))
                        .addLast(new IdleStateHandler(300, 0, 0))
                        .addLast(new ProtocolIdleTimeoutHandler(readIdleListener, writeIdleListener))
                        .addLast(BaseFrameCodec.clientFrameCodec())
                        .addLast(new ProtocolCodec(transfer, false))
                        .addLast(new ChannelProtocolHandler(protocolHandler, sessionBuilder, channelActiveListener, channelInactiveListener, exceptionHandler));
            }
        });
        if (timeout > 0) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        }
        ChannelFuture cf = bootstrap.connect(address);
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("connect to remote server success: {}", address);
                    channel = channelFuture.channel();
                    latch.countDown();
                } else {
                    log.error("connect to remote server time out: {}", address);
                    throw new RuntimeException("connect to remote server time out: " + address);
                }
            }
        });
    }

    @Override
    public void bind() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        log.info("client closing...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }

        channel.close();
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public boolean isActive() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }
        return channel.isActive();
    }

    public void request(AbstractProtocol protocol) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }

        channel.writeAndFlush(protocol);
    }

    //setter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setSessionBuilder(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
    }

    public void setChannelActiveListener(ChannelActiveListener channelActiveListener) {
        this.channelActiveListener = channelActiveListener;
    }

    public void setChannelInactiveListener(ChannelInactiveListener channelInactiveListener) {
        this.channelInactiveListener = channelInactiveListener;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void setReadIdleListener(ReadIdleListener readIdleListener) {
        this.readIdleListener = readIdleListener;
    }

    public void setWriteIdleListener(WriteIdleListener writeIdleListener) {
        this.writeIdleListener = writeIdleListener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Client that = (Client) o;

        return channel != null ? channel.equals(that.channel) : that.channel == null;
    }

    @Override
    public int hashCode() {
        return channel != null ? channel.hashCode() : 0;
    }
}
