package org.kin.kinrpc.transport.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.kinrpc.transport.*;
import org.kin.kinrpc.transport.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.handler.ChannelIdleHandler;
import org.kin.kinrpc.transport.handler.ChannelProtocolHandler;
import org.kin.kinrpc.transport.handler.ProtocolCodec;
import org.kin.kinrpc.transport.listener.ChannelActiveListener;
import org.kin.kinrpc.transport.listener.ChannelIdleListener;
import org.kin.kinrpc.transport.listener.ChannelInactiveListener;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public class Client extends AbstractConnection {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    private volatile Channel channel;
    private volatile boolean isStopped;
    //连接超时毫秒数
    private int timeout;

    private final Bytes2ProtocolTransfer transfer;
    private final ProtocolHandler protocolHandler;
    private SessionBuilder sessionBuilder = new DefaultSessionBuilder();
    private ChannelActiveListener channelActiveListener;
    private ChannelInactiveListener channelInactiveListener;
    private ChannelExceptionHandler channelExceptionHandler;
    private ChannelIdleListener channelIdleListener;

    public Client(InetSocketAddress address, Bytes2ProtocolTransfer transfer, ProtocolHandler protocolHandler) {
        super(address);
        this.transfer = transfer;
        this.protocolHandler = protocolHandler;
    }

    @Override
    public void connect() {
        log.info("client connecting...");
        CountDownLatch latch = new CountDownLatch(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new WriteTimeoutHandler(3));
                if(channelIdleListener != null){
                    int readIdleTime = channelIdleListener.readIdleTime();
                    int writeIdleTime = channelIdleListener.writeIdelTime();
                    int allIdleTime = channelIdleListener.allIdleTime();
                    if(readIdleTime > 0 || writeIdleTime > 0 || allIdleTime > 0){
                        //其中一个>0就设置Handler
                        socketChannel.pipeline()
                                .addLast(new IdleStateHandler(readIdleTime, writeIdleTime, allIdleTime))
                                .addLast(new ChannelIdleHandler(channelIdleListener));
                    }

                }

                socketChannel.pipeline()
                        .addLast(BaseFrameCodec.clientFrameCodec())
                        .addLast(new ProtocolCodec(transfer, false))
                        .addLast(new ChannelProtocolHandler(protocolHandler, sessionBuilder, channelActiveListener, channelInactiveListener, channelExceptionHandler));
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
                    latch.countDown();
                    throw new RuntimeException("connect to remote server time out: " + address);
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {

        }
    }

    @Override
    public void bind() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        isStopped = true;
        if(channel != null){
            channel.close();
        }
        eventLoopGroup.shutdownGracefully();
        log.info("client closed");
    }

    @Override
    public boolean isActive() {
        return !isStopped && channel != null && channel.isActive();
    }

    public void request(AbstractProtocol protocol) {
        if(isActive()){
            channel.writeAndFlush(protocol.write());
        }
    }

    public String getLocalAddress(){
        if(channel != null){
            return channel.localAddress().toString();
        }
        return "";
    }

    //setter && getter
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

    public void setChannelExceptionHandler(ChannelExceptionHandler channelExceptionHandler) {
        this.channelExceptionHandler = channelExceptionHandler;
    }

    public void setChannelIdleListener(ChannelIdleListener channelIdleListener) {
        this.channelIdleListener = channelIdleListener;
    }

    public boolean isStopped() {
        return isStopped;
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
