package org.kin.kinrpc.transport.rsocket;

import io.netty.buffer.ByteBuf;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.RemotingProcessor;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ReactorNetty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketResponder implements RSocket {
    /** 默认获取request future方法 */
    private static final Function<Long, CompletableFuture<Object>> DEFAULT_REQUEST_FUTURE_FUNC = id -> null;

    /** rsocket requester */
    private final RSocket requester;
    /** remoting command processor */
    private final RemotingProcessor remotingProcessor;
    /** 获取request future方法 */
    private final Function<Long, CompletableFuture<Object>> requestFutureFunc;
    /** remote address */
    private SocketAddress remoteAddress;

    public RSocketResponder(RSocket requester, RemotingProcessor remotingProcessor) {
        this(requester, remotingProcessor, DEFAULT_REQUEST_FUTURE_FUNC);
    }

    public RSocketResponder(RSocket requester, RemotingProcessor remotingProcessor, Function<Long, CompletableFuture<Object>> requestFutureFunc) {
        this.requester = requester;
        this.remotingProcessor = remotingProcessor;
        this.requestFutureFunc = requestFutureFunc;

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
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        Sinks.One<Payload> sink = Sinks.one();

        try{
            remotingProcessor.process(new ChannelContext() {
                @Override
                public void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener) {
                    sink.emitValue(ByteBufPayload.create(byteBuf), RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                    // TODO: 2023/6/8 无法监听error
                    listener.onComplete();
                }

                @Override
                public SocketAddress address() {
                    return remoteAddress;
                }
            }, payload.data().retain());
        }catch (Exception e){
            sink.emitError(e, RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
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
