package org.kin.kinrpc.remoting.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.remoting.transport.handler.codec.impl.RPCDecoder;
import org.kin.kinrpc.remoting.transport.handler.codec.impl.RPCEncoder;
import org.kin.kinrpc.remoting.transport.handler.ClientHandler;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.serializer.impl.Hessian2Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ClientConnection extends Connection {
    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);
    //所有的消费者共用一个EventLoopGroup
    private final EventLoopGroup eventLoopGroup;
    //响应Channel连接的ChannelFuture
    private ChannelFuture channelFuture;
    //针对该连接的ReferenceHandler
    private volatile ClientHandler handler;

    //建立好连接后才可以进行一些操作
    private CountDownLatch latch = new CountDownLatch(1);

    //连接超时毫秒数
    private final int timeout;

    public ClientConnection(InetSocketAddress address, EventLoopGroup eventLoopGroup, int timeout) {
        super(address);
        this.eventLoopGroup = eventLoopGroup;
        this.timeout = timeout;
    }

    @Override
    public void connect() {
        log.info("ClientConnection connecting...");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new RPCEncoder(new Hessian2Serializer()))
                        .addLast(new LengthFieldBasedFrameDecoder(Constants.FRAME_MAX_LENGTH, Constants.FRAMELENGTH_FIELD_OFFSET, Constants.FRAMELENGTH_FIELD_LENGTH, 0, 0))
                        .addLast(new RPCDecoder(new Hessian2Serializer()))
                        .addLast(new ClientHandler());
            }
        });
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        this.channelFuture = bootstrap.connect(address);
        channelFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("Successfully connect to remote server. remote peer = " + address);
                    handler = channelFuture.channel().pipeline().get(ClientHandler.class);
                    latch.countDown();
                } else {
                    log.error("connect to Server time out");
                    throw new RuntimeException("connect to Server time out");
                }
            }
        });
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException();
    }

    @Override
    /**
     * 使用latch,故不存在null对象问题,因为没连接好就会阻塞
     */
    public void close() {
        log.info("ClientConnection closing...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channelFuture.channel().close();
    }

    public RPCFuture request(RPCRequest request) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return handler.request(request);
    }

}
