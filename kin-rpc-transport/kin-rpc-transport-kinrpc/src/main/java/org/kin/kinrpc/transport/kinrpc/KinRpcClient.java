package org.kin.kinrpc.transport.kinrpc;

import io.netty.buffer.ByteBuf;
import io.netty.util.NetUtil;
import org.kin.kinrpc.transport.AbsRemotingClient;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.transport.netty.ChannelOperationListener;
import org.kin.transport.netty.ClientObserver;
import org.kin.transport.netty.Session;
import org.kin.transport.netty.tcp.client.TcpClient;
import org.kin.transport.netty.tcp.client.TcpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/3
 */
public class KinRpcClient extends AbsRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(KinRpcClient.class);


    /** tcp client transport config */
    private final TcpClientTransport transport;
    /** tcp client */
    private volatile TcpClient client;

    public KinRpcClient(int port) {
        this(NetUtil.LOCALHOST.getHostAddress(), port);
    }

    public KinRpcClient(String host, int port) {
        super(host, port);
        transport = TcpClientTransport.create()
                .payloadProcessor((s, bp) ->
                        Mono.fromRunnable(() -> remotingProcessor.process(new ChannelContext() {
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

                            @Nullable
                            @Override
                            public CompletableFuture<Object> removeRequestFuture(long requestId) {
                                return KinRpcClient.this.removeRequestFuture(requestId);
                            }
                        }, bp.data().retain())))
                .observer(new ClientObserver<TcpClient>() {
                    @Override
                    public void onConnected(TcpClient client, Session session) {
                        log.info("kinrpc client connect to {}:{} success", host, port);
                    }

                    @Override
                    public void onDisconnected(TcpClient client, @Nullable Session session) {
                        log.info("kinrpc client(- R:{}:{}) terminated", host, port);
                    }
                });
    }

    @Override
    public void start() {
        if (client != null) {
            throw new IllegalStateException(String.format("kinrpc client has been connect to %s:%d", host, port));
        }
        client = transport.connect(host, port);
    }

    @Override
    public void shutdown() {
        checkStarted();

        if (client.isDisposed()) {
            return;
        }
        remotingProcessor.shutdown();
        client.dispose();
    }

    /**
     * 检查client是否started
     */
    private void checkStarted() {
        if (Objects.isNull(client)) {
            throw new IllegalStateException(String.format("kinrpc client does not start to connect to %s:%d", host, port));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        if (Objects.isNull(command)) {
            throw new IllegalArgumentException("request command is null");
        }

        checkStarted();

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        client.send(codec.encode(command), new ChannelOperationListener() {
            @Override
            public void onFailure(Session session, Throwable cause) {
                removeRequestFuture(command.getId());
                requestFuture.completeExceptionally(cause);
            }
        }).subscribe();

        return (CompletableFuture<T>) requestFuture;
    }
}
