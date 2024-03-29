package org.kin.kinrpc.transport.rsocket;

import io.netty.buffer.Unpooled;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.AbstractRemotingClient;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.transport.netty.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ReactorNetty;
import reactor.netty.tcp.TcpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketClient extends AbstractRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(RSocketClient.class);

    /** ssl配置 */
    private final SslConfig sslConfig;
    /** rsocket requester */
    private volatile Mono<RSocket> requesterMono;

    public RSocketClient(int port) {
        this(port, null);
    }

    public RSocketClient(int port, SslConfig sslConfig) {
        this(NetUtils.getLocalhost4Ip(), port, sslConfig);
    }

    public RSocketClient(String host, int port, @Nullable SslConfig sslConfig) {
        super(host, port);
        this.sslConfig = sslConfig;
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

        TcpClient tcpClient = TcpClient.create()
                .host(host)
                .port(port);
        if (Objects.nonNull(sslConfig)) {
            //ssl
            tcpClient = tcpClient.secure(scs -> scs.sslContext(SslUtils.getClientSslContext(
                    sslConfig.getCertFile(), sslConfig.getCertKeyFile(), sslConfig.getCertKeyPassword(),
                    sslConfig.getCaFile(), sslConfig.getFingerprintFile())));
        }

        RSocketConnector rsocketConnector = RSocketConnector.create();
        //user custom
        for (RSocketClientCustomizer customizer : ExtensionLoader.getExtensions(RSocketClientCustomizer.class)) {
            customizer.custom(rsocketConnector);
        }
        //internal, user can not modify
        rsocketConnector.setupPayload(ByteBufPayload.create(Unpooled.EMPTY_BUFFER))
                //zero copy
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .connect(TcpClientTransport.create(tcpClient))
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
                })
                .subscribe();
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
