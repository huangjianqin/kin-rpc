package org.kin.kinrpc.transport.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.kinrpc.serializer.impl.Hessian2Serializer;
import org.kin.kinrpc.transport.Connection;
import org.kin.kinrpc.transport.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.rpc.handler.ProviderHandler;
import org.kin.kinrpc.transport.rpc.handler.RPCDecoder;
import org.kin.kinrpc.transport.rpc.handler.RPCEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderConnection extends Connection {
    private static final Logger log = LoggerFactory.getLogger("transport");
    private final RPCServer rpcServer;
    //连接相关属性
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel selector;

    public ProviderConnection(InetSocketAddress address, RPCServer rpcServer) {
        super(address);
        this.rpcServer = rpcServer;
    }

    @Override
    public void connect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind() throws Exception {
        log.info("provider connection binding...");
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
                                .addLast(BaseFrameCodec.serverRPCFrameCodec())
                                .addLast(new RPCDecoder(new Hessian2Serializer()))
                                .addLast(new ProviderHandler(rpcServer))
                                .addLast(new RPCEncoder(new Hessian2Serializer()));
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
                    log.error("server connection bind fail: {}", address);
                    throw new RuntimeException("server connection bind fail: " + address);
                }
            }
        });

        latch.await();
    }

    @Override
    public void close() {
        if (this.selector == null || this.workerGroup == null || this.bossGroup == null) {
            log.error("provider connection has not started call close");
            throw new IllegalStateException("provider connection has not started");
        }

        log.info("provider connection close");
        this.selector.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();

        this.selector = null;
        this.workerGroup = null;
        this.bossGroup = null;
    }

}
