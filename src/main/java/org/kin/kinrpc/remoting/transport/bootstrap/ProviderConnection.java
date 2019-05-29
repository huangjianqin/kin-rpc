package org.kin.kinrpc.remoting.transport.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.log4j.Logger;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.rpc.protocol.serializer.Hessian2Serializer;
import org.kin.kinrpc.remoting.transport.handler.common.RPCDecoder;
import org.kin.kinrpc.remoting.transport.handler.common.RPCEncoder;
import org.kin.kinrpc.remoting.transport.handler.provider.ProviderHandler;

import java.net.InetSocketAddress;

import java.util.concurrent.BlockingQueue;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderConnection extends Connection {
    private static final Logger log = Logger.getLogger(ProviderConnection.class);

    private final BlockingQueue<RPCRequest> requestsQueue;

    //连接相关属性
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    //响应Channel关闭的ChannelFuture
    private ChannelFuture channelFuture;

    public ProviderConnection(InetSocketAddress address, BlockingQueue<RPCRequest> requestsQueue) {
        super(address);
        this.requestsQueue = requestsQueue;
    }

    @Override
    public void connect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind() {
        log.info("server connection binding...");
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this.bossGroup, this.workerGroup).channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline()
                            .addLast(new LengthFieldBasedFrameDecoder(Constants.FRAME_MAX_LENGTH, Constants.FRAMELENGTH_FIELD_OFFSET, Constants.FRAMELENGTH_FIELD_LENGTH, 0, 0))
                            .addLast(new RPCDecoder(new Hessian2Serializer()))
                            .addLast(new RPCEncoder(new Hessian2Serializer()))
                            .addLast(new ProviderHandler(requestsQueue));
                }
            });
            this.channelFuture = bootstrap.bind(super.address).sync();
            log.info("server connection bind successfully");
            this.channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            //重试连接
            log.info("retry to connect to the remote:" + address.toString());
            bind();
        }
    }

    @Override
    public void close() {
        if(this.channelFuture == null || this.workerGroup == null || this.bossGroup == null){
            log.error("Provider connection has not started call close");
            throw new IllegalStateException("Provider connection has not started");
        }

        log.info("server connection closing...");
        try {
            this.channelFuture.channel().close().sync();
            this.workerGroup.shutdownGracefully().sync();
            this.bossGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        System.out.println(this.channelFuture.channel().isOpen());
//        System.out.println(this.workerGroup.isShutdown());
//        System.out.println(this.bossGroup.isShutdown());
    }

}
