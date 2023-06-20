package org.kin.kinrpc.transport.rsocket;

import io.netty.buffer.Unpooled;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.transport.AbsRemotingClient;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ReactorNetty;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketClient extends AbsRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(RSocketClient.class);

    /** rsocket requester */
    private volatile Mono<RSocket> requesterMono;

    public RSocketClient(int port) {
        this(NetUtils.getLocalhostIp(), port);
    }

    public RSocketClient(String host, int port) {
        super(host, port);
    }

    @Override
    public void start() {
        if (Objects.nonNull(requesterMono)) {
            throw new IllegalStateException(String.format("rsocket client has been connect to %s:%d", host, port));
        }

        Sinks.One<RSocket> sink = Sinks.one();
        this.requesterMono = sink.asMono();

        RSocketConnector rsocketConnector = RSocketConnector.create();
        //user custom
        for (RSocketClientCustomizer customizer : ExtensionLoader.getExtensions(RSocketClientCustomizer.class)) {
            customizer.custom(rsocketConnector);
        }
        //internal, user can not modify
        rsocketConnector.setupPayload(ByteBufPayload.create(Unpooled.EMPTY_BUFFER))
                //zero copy
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .connect(TcpClientTransport.create(host, port))
                .subscribe(rsocket -> {
                    log.info("rsocket client connect to {}:{} success", host, port);
                    sink.emitValue(rsocket, RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                });
    }

    /**
     * 检查client是否started
     */
    private void checkStarted() {
        if (Objects.isNull(requesterMono)) {
            throw new IllegalStateException(String.format("rsocket client does not start to connect to %s:%d", host, port));
        }
    }

    /**
     * request之前的操作, 一般用于检查
     *
     * @param command request command
     */
    private void beforeRequest(RequestCommand command) {
        if (Objects.isNull(command)) {
            throw new IllegalArgumentException("request command is null");
        }

        checkStarted();
    }

    @Override
    public void shutdown() {
        checkStarted();

        requesterMono.flatMap(rsocket -> {
                    if (rsocket.isDisposed()) {
                        return Mono.empty();
                    }

                    rsocket.dispose();
                    remotingProcessor.shutdown();
                    return rsocket.onClose()
                            .doOnNext(v -> log.info("rsocket client(- R:{}:{}) terminated", host, port));
                })
                .subscribe();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        beforeRequest(command);

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());

        requesterMono.flatMap(rsocket -> rsocket.requestResponse(ByteBufPayload.create(codec.encode(command)))
                        .doOnError(t -> {
                            removeRequestFuture(command.getId());
                            requestFuture.completeExceptionally(t);
                        })
                        .doOnNext(p -> {
                            try {
                                remotingProcessor.process(clientChannelContext, p.data().retain());
                            } finally {
                                ReactorNetty.safeRelease(p);
                            }
                        }))
                .subscribe();

        return (CompletableFuture<T>) requestFuture;
    }

    @Override
    public void fireAndForget(@Nonnull RequestCommand command) {
        beforeRequest(command);

        requesterMono.flatMap(rsocket -> rsocket.fireAndForget(ByteBufPayload.create(codec.encode(command))))
                .subscribe();
    }
}
