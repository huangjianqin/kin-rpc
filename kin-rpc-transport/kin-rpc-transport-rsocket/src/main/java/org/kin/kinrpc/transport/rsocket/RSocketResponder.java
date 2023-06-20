package org.kin.kinrpc.transport.rsocket;

import io.netty.buffer.ByteBuf;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.RemotingProcessor;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ReactorNetty;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketResponder implements RSocket {
    /** rsocket requester */
    private final RSocket requester;
    /** remoting command processor */
    private final RemotingProcessor remotingProcessor;
    /** remote address */
    private SocketAddress remoteAddress;

    public RSocketResponder(RSocket requester, RemotingProcessor remotingProcessor) {
        this.requester = requester;
        this.remotingProcessor = remotingProcessor;

        //通过反射获取remote address
        try {
            Method getDuplexConnectionMethod = requester.getClass().getMethod("getDuplexConnection");
            getDuplexConnectionMethod.setAccessible(true);
            DuplexConnection connection = (DuplexConnection) getDuplexConnectionMethod.invoke(requester);
            remoteAddress = connection.remoteAddress();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        try {
            remotingProcessor.process(new ChannelContext() {
                @Override
                public void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener) {
                    //!!!无法监听transport error
                    listener.onComplete();
                }

                @Override
                public SocketAddress address() {
                    return remoteAddress;
                }
            }, payload.data().retain());
        } finally {
            ReactorNetty.safeRelease(payload);
        }

        return Mono.empty();
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        Sinks.One<Payload> sink = Sinks.one();

        try{
            remotingProcessor.process(new ChannelContext() {
                @Override
                public void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener) {
                    //!!!无法监听transport error
                    sink.emitValue(ByteBufPayload.create(byteBuf), RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                    listener.onComplete();
                }

                @Override
                public SocketAddress address() {
                    return remoteAddress;
                }
            }, payload.data().retain());
        }finally {
            ReactorNetty.safeRelease(payload);
        }

        return sink.asMono();
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return Flux.error(new UnsupportedOperationException());
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public double availability() {
        return requester.availability();
    }

    @Override
    public void dispose() {
        requester.dispose();
    }

    @Override
    public boolean isDisposed() {
        return requester.isDisposed();
    }

    @Override
    public Mono<Void> onClose() {
        return requester.onClose();
    }
}
