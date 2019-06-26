package org.kin.kinrpc.transport.protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.listener.*;
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
public class Server extends AbstractConnection {
    private static final Logger log = LoggerFactory.getLogger("transport");

    //连接相关属性
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel selector;

    //各种处理器
    private final Bytes2ProtocolTransfer transfer;
    private final ProtocolHandler protocolHandler;
    private SessionBuilder sessionBuilder = new DefaultSessionBuilder();
    private ChannelActiveListener channelActiveListener;
    private ChannelInactiveListener channelInactiveListener;
    private ExceptionHandler exceptionHandler;
    private ReadIdleListener readIdleListener;
    private WriteIdleListener writeIdleListener;

    public Server(
            InetSocketAddress address,
            Bytes2ProtocolTransfer transfer,
            ProtocolHandler protocolHandler) {
        super(address);
        this.transfer = transfer;
        this.protocolHandler = protocolHandler;
    }

    @Override
    public void connect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind() throws Exception {
        log.info("server({}) connection binding...", address);
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        CountDownLatch latch = new CountDownLatch(1);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new WriteTimeoutHandler(10))
                                .addLast(new IdleStateHandler(300, 0, 0))
                                .addLast(new ProtocolIdleTimeoutHandler(readIdleListener, writeIdleListener))
                                .addLast(BaseFrameCodec.serverFrameCodec())
                                .addLast(new ProtocolCodec(transfer, true))
                                .addLast(new ChannelProtocolHandler(protocolHandler, sessionBuilder, channelActiveListener, channelInactiveListener, exceptionHandler));

                    }
                });
        ChannelFuture cf = bootstrap.bind(super.address);
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("server connection binded: {}", address);
                    selector = channelFuture.channel();
                    latch.countDown();
                } else {
                    throw new RuntimeException("server connection bind fail: " + address);
                }
            }
        });

        latch.await();
    }

    @Override
    public void close() {
        if (this.selector == null || this.workerGroup == null || this.bossGroup == null) {
            throw new IllegalStateException("server connection has not started");
        }

        this.selector.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();

        this.selector = null;
        this.workerGroup = null;
        this.bossGroup = null;

        log.info("server connection closed");
    }

    @Override
    public boolean isActive() {
        return selector.isActive();
    }

    //setter && getter
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

        Server that = (Server) o;

        return selector != null ? selector.equals(that.selector) : that.selector == null;
    }

    @Override
    public int hashCode() {
        return selector != null ? selector.hashCode() : 0;
    }
}
