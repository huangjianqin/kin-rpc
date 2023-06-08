package org.kin.kinrpc.transport.rsocket;

import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.transport.AbsRemotingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class RSocketServer extends AbsRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(RSocketServer.class);

    /** rsocket server disposable */
    private volatile Mono<CloseableChannel> closeableChannelMono;

    public RSocketServer(int port) {
        this(NetUtils.getLocalhostIp(), port);
    }

    public RSocketServer(String host, int port) {
        super(host, port);
    }

    @Override
    public void start() {
        if (Objects.nonNull(closeableChannelMono)) {
            throw new IllegalStateException(String.format("rsocket server has been started on %s:%d", host, port));
        }

        Sinks.One<CloseableChannel> sink = Sinks.one();
        closeableChannelMono = sink.asMono();

        TcpServerTransport transport = TcpServerTransport.create(host, port);
        io.rsocket.core.RSocketServer.create()
                .acceptor((setup, requester) -> Mono.just(new RSocketResponder(requester, remotingProcessor)))
                //zero copy
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bind(transport)
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
                    .doOnNext(v -> log.info("rsocket server({}:{}) shutdown", host, port));
        }).subscribe();
    }
}
