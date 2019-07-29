package org.kin.kinrpc.transport.netty;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.domain.NettyTransportOption;
import org.kin.kinrpc.transport.listener.ChannelIdleListener;
import org.kin.kinrpc.transport.netty.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.netty.handler.ChannelIdleHandler;
import org.kin.kinrpc.transport.netty.handler.ChannelProtocolHandler;
import org.kin.kinrpc.transport.netty.handler.ProtocolCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public class Server extends AbstractConnection<NettyTransportOption> {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    //连接相关线程池
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel selector;
    public Server(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void connect(NettyTransportOption transportOption) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(NettyTransportOption transportOption) throws Exception {
        log.info("server({}) connection binding...", address);

        Preconditions.checkArgument(bossGroup == null);
        Preconditions.checkArgument(workerGroup == null);
        Preconditions.checkArgument(transportOption.getProtocolHandler() != null);

        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        CountDownLatch latch = new CountDownLatch(1);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup).channel(NioServerSocketChannel.class);

        for(Map.Entry<ChannelOption, Object> entry: transportOption.getChannelOptions().entrySet()){
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
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
                                .addLast(BaseFrameCodec.serverFrameCodec())
                                .addLast(new ProtocolCodec(transportOption.getProtocolTransfer(), true))
                                .addLast(new ChannelProtocolHandler(
                                        transportOption.getProtocolHandler(),
                                        transportOption.getSessionBuilder(),
                                        transportOption.getChannelActiveListener(),
                                        transportOption.getChannelInactiveListener(),
                                        transportOption.getChannelExceptionHandler()));

                    }
                });
        ChannelFuture cf = bootstrap.bind(super.address);
        cf.addListener((ChannelFuture channelFuture) -> {
            if (channelFuture.isSuccess()) {
                log.info("server connection binded: {}", address);
                selector = channelFuture.channel();
                latch.countDown();
            } else {
                throw new RuntimeException("server connection bind fail: " + address);
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
