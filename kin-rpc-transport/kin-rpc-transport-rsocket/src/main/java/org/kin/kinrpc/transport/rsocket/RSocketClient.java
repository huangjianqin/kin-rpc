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
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
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
    protected void onConnect() {
        if (Objects.nonNull(requesterMono)) {
            throw new IllegalStateException(String.format("%s has been connect to %s", name(), remoteAddress()));
        }

        onConnect0();
    }

    private void onConnect0() {
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
                    rsocket.onClose()
                            .doOnSuccess(v -> onConnectionClosed())
                            .subscribe();

                    sink.emitValue(rsocket, RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                    onConnectSuccess();
                }, t -> {
                    sink.emitEmpty(RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                    onConnectFail(t);
                });
    }

    @Override
    protected void onReconnect() {
        requesterMono.doOnSuccess(r -> {
                    if (Objects.nonNull(r) && !r.isDisposed()) {
                        r.dispose();
                    }
                    onConnect0();
                })
                .subscribe();
    }

    @Override
    protected void onShutdown() {
        if (Objects.isNull(requesterMono)) {
            return;
        }

        requesterMono.doOnSuccess(rsocket -> {
                    if (Objects.isNull(rsocket)) {
                        return;
                    }

                    if (rsocket.isDisposed()) {
                        return;
                    }

                    rsocket.dispose();
                    remotingProcessor.shutdown();
                })
                .subscribe();
    }

    /**
     * request之前的操作, 一般用于检查
     *
     * @param command request command
     */
    private void beforeRequest(RemotingCommand command) {
        if (Objects.isNull(command)) {
            throw new IllegalArgumentException("request command is null");
        }
    }

    @Override
    protected CompletableFuture<Void> heartbeat() {
        return requestResponse0(new HeartbeatCommand());
    }

    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        return requestResponse0(command);
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> requestResponse0(RemotingCommand command) {
        beforeRequest(command);

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());

        requesterMono.flatMap(rsocket -> rsocket.requestResponse(ByteBufPayload.create(codec.encode(command)))
                        .doOnError(t -> {
                            removeRequestFuture(command.getId());
                            requestFuture.completeExceptionally(t);

                            if (command instanceof RequestCommand) {
                                onRequestFail(t);
                            }
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
    public CompletableFuture<Void> fireAndForget(@Nonnull RequestCommand command) {
        beforeRequest(command);

        CompletableFuture<Void> signal = new CompletableFuture<>();
        requesterMono.flatMap(rsocket -> rsocket.fireAndForget(ByteBufPayload.create(codec.encode(command))))
                .doOnError(t -> {
                    signal.completeExceptionally(t);
                    onRequestFail(t);
                })
                .doOnSuccess(v -> signal.complete(null))
                .subscribe();

        return signal;
    }
}
