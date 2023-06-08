package org.kin.kinrpc.transport.kinrpc;

import io.netty.buffer.ByteBuf;
import io.netty.util.NetUtil;
import org.kin.kinrpc.transport.AbsRemotingServer;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.transport.netty.ChannelOperationListener;
import org.kin.transport.netty.Session;
import org.kin.transport.netty.tcp.server.TcpServer;
import org.kin.transport.netty.tcp.server.TcpServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/3
 */
public class KinRpcServer extends AbsRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(KinRpcServer.class);

    /** tcp server transport config */
    private final TcpServerTransport transport;
    /** tcp server */
    private volatile TcpServer server;

    public KinRpcServer(int port) {
        this(NetUtil.LOCALHOST.getHostAddress(), port);
    }

    public KinRpcServer(String host, int port) {
        super(host, port);
        transport = TcpServerTransport.create()
                .payloadProcessor((s, bp) -> Mono.fromRunnable(() -> {
                    remotingProcessor.process(new ChannelContext() {
                        @Override
                        public void writeAndFlush(Object msg, @Nullable TransportOperationListener listener) {
                            if (!(msg instanceof ByteBuf)) {
                                throw new TransportException(String.format("illegal outbound message type '%s'", msg.getClass()));
                            }

                            s.send((ByteBuf) msg, new ChannelOperationListener() {
                                        @Override
                                        public void onSuccess(Session session) {
                                            if (Objects.isNull(listener)) {
                                                return;
                                            }
                                            listener.onComplete();
                                        }

                                        @Override
                                        public void onFailure(Session session, Throwable cause) {
                                            if (Objects.isNull(listener)) {
                                                return;
                                            }
                                            listener.onFailure(cause);
                                        }
                                    })
                                    .subscribe();
                        }

                        @Override
                        public SocketAddress address() {
                            return s.remoteAddress();
                        }
                    }, bp.data().retain());
                }));
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
        server.dispose();
        // TODO: 2023/6/7 整合dispose
        remotingProcessor.shutdown();
    }
}
