package org.kin.kinrpc.transport.kinrpc;

import io.netty.buffer.ByteBuf;
import io.netty.util.NetUtil;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.AbstractRemotingServer;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.transport.netty.ChannelOperationListener;
import org.kin.transport.netty.ServerObserver;
import org.kin.transport.netty.Session;
import org.kin.transport.netty.tcp.server.TcpServer;
import org.kin.transport.netty.tcp.server.TcpServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/3
 */
public class KinRpcServer extends AbstractRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(KinRpcServer.class);

    /** tcp server transport config */
    private final TcpServerTransport transport;
    /** tcp server */
    private volatile TcpServer server;

    public KinRpcServer(int port) {
        this(port, null);
    }

    public KinRpcServer(int port,
                        SslConfig sslConfig) {
        this(port, null, sslConfig);
    }

    public KinRpcServer(int port,
                        ManagedExecutor executor,
                        SslConfig sslConfig) {
        this(NetUtil.LOCALHOST.getHostAddress(), port, executor, sslConfig);
    }

    public KinRpcServer(String host,
                        int port,
                        @Nullable ManagedExecutor executor,
                        @Nullable SslConfig sslConfig) {
        super(host, port, executor);
        transport = TcpServerTransport.create()
                .payloadProcessor((s, bp) ->
                        Mono.fromRunnable(() -> remotingProcessor.process(new ChannelContext() {
                            @Override
                            public void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener) {
                                s.send(byteBuf, new ChannelOperationListener() {
                                            @Override
                                            public void onSuccess(Session session) {
                                                listener.onComplete();
                                            }

                                            @Override
                                            public void onFailure(Session session, Throwable cause) {
                                               listener.onFailure(cause);
                                            }
                                        })
                                        .subscribe();
                            }

                            @Override
                            public SocketAddress address() {
                                return s.remoteAddress();
                            }
                        }, bp.data().retain())))
                .observer(new ServerObserver<TcpServer>() {
                    @Override
                    public void onBound(TcpServer server) {
                        log.info("kinrpc server started on {}:{}", host, port);
                    }

                    @Override
                    public void onUnbound(TcpServer server) {
                        log.info("kinrpc server({}:{}) terminated", host, port);
                    }
                });

        if (Objects.nonNull(sslConfig)) {
            //配置ssl
            transport.certFile(sslConfig.getCertFile())
                    .certKeyFile(sslConfig.getCertKeyFile())
                    .certKeyPassword(sslConfig.getCertKeyPassword())
                    .caFile(sslConfig.getCaFile())
                    .fingerprintFile(sslConfig.getFingerprintFile());
        }
    }

    @Override
    public void start() {
        if (server != null) {
            return;
        }
        server = transport.bind(host, port);
    }

    @Override
    public void shutdown() {
        if (server.isDisposed()) {
            return;
        }
        remotingProcessor.shutdown();
        server.dispose();
    }
}
