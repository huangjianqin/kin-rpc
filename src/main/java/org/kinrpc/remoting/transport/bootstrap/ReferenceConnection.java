package org.kinrpc.remoting.transport.bootstrap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.log4j.Logger;
import org.kinrpc.common.Constants;
import org.kinrpc.remoting.transport.handler.common.RPCDecoder;
import org.kinrpc.remoting.transport.handler.common.RPCEncoder;
import org.kinrpc.remoting.transport.handler.consumer.ReferenceHandler;
import org.kinrpc.rpc.future.RPCFuture;
import org.kinrpc.rpc.protol.RPCRequest;
import org.kinrpc.rpc.protol.srializer.Hessian2Serializer;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceConnection extends Connection {
    private static final Logger log = Logger.getLogger(ReferenceConnection.class);
    //所有的消费者共用一个EventLoopGroup
    private EventLoopGroup eventLoopGroup;
    //响应Channel连接的ChannelFuture
    private ChannelFuture channelFuture;
    //针对该连接的ReferenceHandler
    private volatile ReferenceHandler handler;

    //建立好连接后才可以进行一些操作
    private CountDownLatch latch = new CountDownLatch(1);

    public ReferenceConnection(InetSocketAddress address, EventLoopGroup eventLoopGroup) {
        super(address);
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public void connect() {
        log.info("ReferenceConnection connecting...");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new RPCEncoder(new Hessian2Serializer()))
                        .addLast(new LengthFieldBasedFrameDecoder(Constants.FRAME_MAX_LENGTH, Constants.FRAMELENGTH_FIELD_OFFSET, Constants.FRAMELENGTH_FIELD_LENGTH, 0, 0))
                        .addLast(new RPCDecoder(new Hessian2Serializer()))
                        .addLast(new ReferenceHandler());
            }
        });
        this.channelFuture = bootstrap.connect(address);
        channelFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.isSuccess()){
                    log.info("Successfully connect to remote server. remote peer = " + address);
                    handler = channelFuture.channel().pipeline().get(ReferenceHandler.class);
                    latch.countDown();
                }
            }
        });
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        log.info("ReferenceConnection closing...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channelFuture.channel().close();
    }

    public RPCFuture request(RPCRequest request){
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return handler.request(request);
    }

}
