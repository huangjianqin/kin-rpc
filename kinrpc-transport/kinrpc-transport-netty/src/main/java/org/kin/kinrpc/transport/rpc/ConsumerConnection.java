package org.kin.kinrpc.transport.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.serializer.impl.Hessian2Serializer;
import org.kin.kinrpc.transport.AbstractConsumerConnection;
import org.kin.kinrpc.transport.handler.BaseFrameCodec;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.kin.kinrpc.transport.rpc.handler.ConsumerHandler;
import org.kin.kinrpc.transport.rpc.handler.RPCDecoder;
import org.kin.kinrpc.transport.rpc.handler.RPCEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ConsumerConnection extends AbstractConsumerConnection {
    private static final Logger log = LoggerFactory.getLogger("transport");
    //所有的消费者共用一个EventLoopGroup
    private final EventLoopGroup eventLoopGroup;
    private Channel channel;
    //针对该连接的ReferenceHandler
    private volatile ConsumerHandler handler;

    //建立好连接后才可以进行一些操作
    private CountDownLatch latch = new CountDownLatch(1);

    //连接超时毫秒数
    private final int timeout;

    public ConsumerConnection(InetSocketAddress address, EventLoopGroup eventLoopGroup, int timeout) {
        super(address);
        this.eventLoopGroup = eventLoopGroup;
        this.timeout = timeout;
    }

    @Override
    public void connect() {
        log.info("consumer connecting...");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new WriteTimeoutHandler(10))
                        .addLast(BaseFrameCodec.clientRPCFrameCodec())
                        .addLast(new RPCDecoder(new Hessian2Serializer()))
                        .addLast(new ConsumerHandler())
                        .addLast(new RPCEncoder(new Hessian2Serializer()));
            }
        });
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        ChannelFuture cf = bootstrap.connect(address);
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("connect to remote server success: {}", address);
                    channel = channelFuture.channel();
                    handler = channel.pipeline().get(ConsumerHandler.class);
                    latch.countDown();
                } else {
                    log.error("connect to remote server time out: {}", address);
                    throw new RuntimeException("connect to remote server time out: " + address);
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        log.info("consumer closing...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }

        if (channel.isOpen() || channel.isActive()) {
            channel.close();
        }
    }

    @Override
    public Future<RPCResponse> request(RPCRequest request) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }

        return handler.request(request);
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConsumerConnection that = (ConsumerConnection) o;

        return channel.equals(that.channel);
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }
}
