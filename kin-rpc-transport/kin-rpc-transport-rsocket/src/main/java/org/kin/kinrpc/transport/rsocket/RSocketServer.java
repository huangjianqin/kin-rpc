package org.kin.kinrpc.transport.rsocket;

import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.AbsRemotingServer;
import org.kin.transport.netty.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpServer;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketServer extends AbsRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(RSocketServer.class);

    /** rsocket server disposable */
    private volatile Mono<CloseableChannel> closeableChannelMono;
    /** ssl配置 */
    private final SslConfig sslConfig;

    public RSocketServer(int port) {
        this(port, null);
    }

    public RSocketServer(int port, SslConfig sslConfig) {
        this(NetUtils.getLocalhostIp(), port, sslConfig);
    }

    public RSocketServer(String host, int port, @Nullable SslConfig sslConfig) {
        super(host, port);
        this.sslConfig = sslConfig;
    }

    @Override
    public void start() {
        if (Objects.nonNull(closeableChannelMono)) {
            throw new IllegalStateException(String.format("rsocket server has been started on %s:%d", host, port));
        }

        Sinks.One<CloseableChannel> sink = Sinks.one();
        closeableChannelMono = sink.asMono();

        TcpServer tcpServer = TcpServer.create()
                .host(host)
                .port(port);
        if (Objects.nonNull(sslConfig)) {
            tcpServer = tcpServer.secure(scs -> scs.sslContext(SslUtils.setUpServerSslContext(
                    sslConfig.getCertFile(), sslConfig.getCertKeyFile(), sslConfig.getCertKeyPassword(),
                    sslConfig.getCaFile(), sslConfig.getFingerprintFile())));
        }

        io.rsocket.core.RSocketServer rsocketServer = io.rsocket.core.RSocketServer.create();
        //user custom
        for (RSocketServerCustomizer customizer : ExtensionLoader.getExtensions(RSocketServerCustomizer.class)) {
            customizer.custom(rsocketServer);
        }
        //internal, user can not modify
        rsocketServer.acceptor((setup, requester) -> Mono.just(new RSocketResponder(requester, remotingProcessor)))
                //zero copy
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bind(TcpServerTransport.create(tcpServer))
                .onTerminateDetach()
                .subscribe(cc -> {
                    log.info("rsocket server started on {}:{}", host, port);
                    sink.emitValue(cc, RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                });
    }

    @Override
    public void shutdown() {
        if (Objects.isNull(closeableChannelMono)) {
            throw new IllegalStateException(String.format("rsocket server does not start on %s:%d", host, port));
        }

        closeableChannelMono.flatMap(cc -> {
            if (cc.isDisposed()) {
                return Mono.empty();
            }

            cc.dispose();
            remotingProcessor.shutdown();
            return cc.onClose()
                    .doOnNext(v -> log.info("rsocket server({}:{}) terminated", host, port));
        }).subscribe();
    }
}
