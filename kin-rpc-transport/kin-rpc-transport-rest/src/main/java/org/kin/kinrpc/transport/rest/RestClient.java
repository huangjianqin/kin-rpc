package org.kin.kinrpc.transport.rest;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.util.NetUtil;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.AbstractRemotingClient;
import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.transport.netty.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/10/28
 */
public class RestClient extends AbstractRemotingClient {
    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    /** fake heartbeat返回值 */
    private static final CompletableFuture<Void> EMPTY_HEARTBEAT = CompletableFuture.completedFuture(null);
    /** http client default {@link LoopResources} */
    private static final LoopResources DEFAULT_LOOP_RESOURCES = LoopResources.create("kin-http-client", SysUtils.CPU_NUM * 4, false);

    static {
        JvmCloseCleaner.instance().add(DEFAULT_LOOP_RESOURCES::dispose);
    }

    /** http connection provider */
    private final ConnectionProvider connectionProvider;
    /** http client */
    private final HttpClient httpClient;

    public RestClient(int port) {
        this(port, null);
    }

    public RestClient(int port, SslConfig sslConfig) {
        this(NetUtil.LOCALHOST.getHostAddress(), port, sslConfig);
    }

    public RestClient(String host, int port, @Nullable SslConfig sslConfig) {
        super(host, port);

        HttpProtocol protocol = HttpProtocol.H2C;
        if (Objects.nonNull(sslConfig)) {
            protocol = HttpProtocol.H2;
        }

        this.connectionProvider = ConnectionProvider.builder("connectionProvider-" + port)
//                .pendingAcquireMaxCount(128)
                //每5min检查连接池是否已经空闲1min
                .disposeInactivePoolsInBackground(Duration.ofMinutes(5), Duration.ofMinutes(1))
                //连接池30s后才真正dispose, 这30s用于等待所有已发送请求响应
                //http2不能开启
//                .disposeTimeout(Duration.ofSeconds(30))
                //后台每3min检查并移除无效连接
                .evictInBackground(Duration.ofMinutes(3))
                .fifo()
                .maxConnections(32)
                .metrics(true)
                .build();
        HttpClient httpClient =
                HttpClient.create(connectionProvider)
                        .host(host)
                        .port(port)
                        .protocol(protocol)
                        .keepAlive(true)
                        .baseUrl("/kinrpc")
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .compress(true)
                        .runOn(DEFAULT_LOOP_RESOURCES);
        if (Objects.nonNull(sslConfig)) {
            //配置ssl
            httpClient = httpClient.secure(sslContextSpec ->
                    sslContextSpec.sslContext(
                            SslUtils.getClientSslContext(
                                    sslConfig.getCertFile(),
                                    sslConfig.getCertKeyFile(),
                                    sslConfig.getCertKeyPassword(),
                                    sslConfig.getCaFile(),
                                    sslConfig.getFingerprintFile())));
        }

        this.httpClient = httpClient;
    }

    @Override
    protected void onConnect() {
        onConnectSuccess(false);
    }

    @Override
    protected void onReconnect() {
        //do nothing
    }

    @Override
    protected void onShutdown() {
        remotingProcessor.shutdown();
        connectionProvider.dispose();
    }

    @Override
    protected CompletableFuture<Void> heartbeat() {
        //fake heartbeat
        return EMPTY_HEARTBEAT;
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

    /**
     * 从{@code command}解析出request uri
     *
     * @param command request command
     * @return request uri
     */
    private String getUri(RequestCommand command) {
        String uri = "/";
        if (command instanceof RpcRequestCommand) {
            uri += ((RpcRequestCommand) command).getHandlerId();
        } else if (command instanceof MessageCommand) {
            uri += ((MessageCommand) command).getInterest();
        } else {
            throw new IllegalArgumentException("does not support " + command.getClass().getSimpleName());
        }

        return uri;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        beforeRequest(command);

        CompletableFuture<Object> requestFuture = createRequestFuture(command.getId());
        httpClient.post()
                .uri(getUri(command))
                .send(Mono.just(codec.encode(command)))
                .response((response, byteBufFlux) ->
                        byteBufFlux.flatMap(byteBuf ->
                                Mono.fromRunnable(() -> remotingProcessor.process(clientChannelContext, byteBuf.retain())))
                )
                .then()
                .doOnError(t -> {
                    removeRequestFuture(command.getId());
                    requestFuture.completeExceptionally(t);
                    onRequestFail(t);
                })
                .subscribe();

        return (CompletableFuture<T>) requestFuture;
    }

    @Override
    public CompletableFuture<Void> fireAndForget(RequestCommand command) {
        beforeRequest(command);

        CompletableFuture<Void> signal = new CompletableFuture<>();
        httpClient.post()
                .uri(getUri(command))
                .send(Mono.just(codec.encode(command)))
                .response()
                .then()
                .doOnSuccess(v -> signal.complete(null))
                .doOnError(t -> {
                    signal.completeExceptionally(t);
                    onRequestFail(t);
                })
                .subscribe();

        return signal;
    }
}
