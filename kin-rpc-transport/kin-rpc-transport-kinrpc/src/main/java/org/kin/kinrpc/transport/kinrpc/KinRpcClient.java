package org.kin.kinrpc.transport.kinrpc;

import io.netty.util.NetUtil;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.AbstractRemotingClient;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
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
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author huangjianqin
 * @date 2023/6/3
 */
public class KinRpcClient extends AbstractRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(KinRpcClient.class);
    /** unhealth exception */
    private static final Predicate<Throwable> UNHEALTH_EXCEPTION = t -> t instanceof IOException;

    /** tcp client transport config */
    private final TcpClientTransport transport;
    /** tcp client */
    private TcpClient client;

    public KinRpcClient(int port) {
        this(port, null);
    }

    public KinRpcClient(int port, SslConfig sslConfig) {
        this(NetUtil.LOCALHOST.getHostAddress(), port, sslConfig);
    }

    public KinRpcClient(String host, int port, @Nullable SslConfig sslConfig) {
        super(host, port);
        transport = TcpClientTransport.create()
                .payloadProcessor((s, bp) ->
                        Mono.fromRunnable(() -> remotingProcessor.process(clientChannelContext, bp.data().retain())))
                .observer(new ClientObserver<TcpClient>() {
                    @Override
                    public void onConnected(TcpClient client, Session session) {
                        onConnectSuccess();
                    }

                    @Override
                    public void onConnectFail(TcpClient client, Throwable cause) {
                        KinRpcClient.this.onConnectFail(cause);
                    }

                    @Override
                    public void onDisconnected(TcpClient client, @Nullable Session session) {
                        KinRpcClient.this.onConnectionClosed();
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
    protected void onConnect() {
        if (client != null) {
            throw new IllegalStateException(String.format("%s has been connect to %s", name(), remoteAddress()));
        }
        onConnect0();
    }

    private void onConnect0() {
        client = transport.connect(host, port);
    }

    @Override
    protected void onReconnect() {
        if (Objects.nonNull(client) && !client.isDisposed()) {
            client.dispose();
        }

        onConnect0();
    }

    @Override
    protected void onShutdown() {
        if (Objects.isNull(client)) {
            return;
        }

        if (client.isDisposed()) {
            return;
        }

        client.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        beforeRequest(command);

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        client.send(codec.encode(command), new ChannelOperationListener() {
            @Override
            public void onFailure(Session session, Throwable cause) {
                removeRequestFuture(command.getId());
                requestFuture.completeExceptionally(cause);
                onRequestFail(cause);
            }
        }).subscribe();

        return (CompletableFuture<T>) requestFuture;
    }

    @Override
    public CompletableFuture<Void> fireAndForget(RequestCommand command) {
        beforeRequest(command);
        CompletableFuture<Void> signal = new CompletableFuture<>();
        client.send(codec.encode(command), new ChannelOperationListener() {
            @Override
            public void onSuccess(Session session) {
                signal.complete(null);
            }

            @Override
            public void onFailure(Session session, Throwable cause) {
                signal.completeExceptionally(cause);
                onRequestFail(cause);
            }
        }).subscribe();
        return signal;
    }

    @Override
    protected CompletableFuture<Void> heartbeat() {
        HeartbeatCommand command = new HeartbeatCommand();

        beforeRequest(command);
        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        client.send(codec.encode(command), new ChannelOperationListener() {
            @Override
            public void onFailure(Session session, Throwable cause) {
                requestFuture.completeExceptionally(cause);
            }
        }).subscribe();
        return CompletableFuture.allOf(requestFuture);
    }

    /**
     * 发送请求失败
     *
     * @param t 发送异常
     */
    protected void onRequestFail(Throwable t) {
        if (!UNHEALTH_EXCEPTION.test(t)) {
            return;
        }

        super.onRequestFail(t);
    }


}
