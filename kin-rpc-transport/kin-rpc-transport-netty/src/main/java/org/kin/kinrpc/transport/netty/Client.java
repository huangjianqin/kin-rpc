package org.kin.kinrpc.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.domain.NettyTransportOption;
import org.kin.kinrpc.transport.listener.ChannelIdleListener;
import org.kin.kinrpc.transport.netty.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.netty.handler.ChannelIdleHandler;
import org.kin.kinrpc.transport.netty.handler.ChannelProtocolHandler;
import org.kin.kinrpc.transport.netty.handler.ProtocolCodec;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public class Client extends AbstractConnection<NettyTransportOption> {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    private volatile Channel channel;
    private volatile boolean isStopped;

    public Client(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void connect(NettyTransportOption transportOption) {
        log.info("client connecting...");
        CountDownLatch latch = new CountDownLatch(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);

        for(Map.Entry<ChannelOption, Object> entry: transportOption.getChannelOptions().entrySet()){
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new WriteTimeoutHandler(3));
                ChannelIdleListener channelIdleListener = transportOption.getChannelIdleListener();
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
                        .addLast(new ProtocolCodec(transportOption.getProtocolTransfer(), false))
                        .addLast(new ChannelProtocolHandler(
                                transportOption.getProtocolHandler(),
                                transportOption.getSessionBuilder(),
                                transportOption.getChannelActiveListener(),
                                transportOption.getChannelInactiveListener(),
                                transportOption.getChannelExceptionHandler()));
            }
        });
        ChannelFuture cf = bootstrap.connect(address);
        cf.addListener((ChannelFuture channelFuture) -> {
            if (channelFuture.isSuccess()) {
                log.info("connect to remote server success: {}", address);
                channel = channelFuture.channel();
                latch.countDown();
            } else {
                latch.countDown();
                throw new RuntimeException("connect to remote server time out: " + address);
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {

        }
    }

    @Override
    public void bind(NettyTransportOption transportOption) throws Exception {
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
