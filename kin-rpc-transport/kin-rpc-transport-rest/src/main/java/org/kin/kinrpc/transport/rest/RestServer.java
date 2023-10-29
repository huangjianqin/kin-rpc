package org.kin.kinrpc.transport.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.util.NetUtil;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.AbstractRemotingServer;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.transport.netty.RetryNonSerializedEmitFailureHandler;
import org.kin.transport.netty.utils.SslUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.resources.LoopResources;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Objects;

/**
 * 本质上是基于HTTP2 server
 *
 * @author huangjianqin
 * @date 2023/10/28
 */
public class RestServer extends AbstractRemotingServer {
    private static final Logger log = LoggerFactory.getLogger(RestServer.class);

    /** http server disposable mono */
    private final Mono<DisposableServer> starter;

    /** http server disposable */
    private volatile DisposableServer disposable;

    public RestServer(int port) {
        this(port, null);
    }

    public RestServer(int port,
                      SslConfig sslConfig) {
        this(port, null, sslConfig);
    }

    public RestServer(int port,
                      ManagedExecutor executor,
                      SslConfig sslConfig) {
        this(NetUtil.LOCALHOST.getHostAddress(), port, executor, sslConfig);
    }

    public RestServer(String host,
                      int port,
                      @Nullable ManagedExecutor executor,
                      @Nullable SslConfig sslConfig) {
        super(host, port, executor);

        HttpServer httpServer = HttpServer.create();

        HttpProtocol protocol = HttpProtocol.H2C;
        if (Objects.nonNull(sslConfig)) {
            //配置ssl
            protocol = HttpProtocol.H2;
            httpServer = httpServer.secure(sslContextSpec ->
                    sslContextSpec.sslContext(
                            SslUtils.getServerSslContext(
                                    sslConfig.getCertFile(),
                                    sslConfig.getCertKeyFile(),
                                    sslConfig.getCertKeyPassword(),
                                    sslConfig.getCaFile(),
                                    sslConfig.getFingerprintFile())));
        }

        LoopResources loopResources = LoopResources.create("kin-http-server-" + port, 2, SysUtils.CPU_NUM * 2, false);
        httpServer = httpServer.host(host)
                .port(port)
                .protocol(protocol)
                //以handlerId为url
                .route(routes -> routes.post("/kinrpc/{interest}", this::handle))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //打印底层event和二进制内容
//                .wiretap(false)
                .accessLog(true)
                //>=256KB+client允许接受压缩就开启压缩
                .compress(true)
                .compress(256 * 1024)
                //1min空闲超时
                .idleTimeout(Duration.ofMinutes(1))
                //最多最存在256个待处理的http request
                .maxKeepAliveRequests(256)
                //自定义event loop
                .runOn(loopResources);

        this.starter =
                httpServer
                        .doOnUnbound(d -> {
                            d.onDispose(loopResources);
                            d.onDispose(() -> log.info("rest server({}:{}) terminated", host, port));
                        })
                        .bind()
                        .doOnSuccess(d -> log.info("rest server started on {}:{}", host, port))
                        .doOnError(t -> log.error("rest server encounter error when starting", t))
                        .cast(DisposableServer.class);
    }

    /**
     * route handle处理
     *
     * @return complete signal
     */
    private Publisher<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        Sinks.One<Void> signal = Sinks.one();
        // TODO: 2023/10/29 是否需要检验特定header
        return Mono.from(request.receive()
                        .flatMap(byteBuf ->
                                Mono.fromRunnable(() -> remotingProcessor.process(new ChannelContext() {
                                    @Override
                                    public void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener) {
                                        response.send(Mono.just(byteBuf))
                                                .then()
                                                .subscribe(s -> {
                                                            signal.emitEmpty(RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                                                            listener.onComplete();
                                                        },
                                                        t -> {
                                                            signal.emitEmpty(RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED);
                                                            listener.onFailure(t);
                                                        });
                                    }

                                    @Override
                                    public SocketAddress address() {
                                        return request.remoteAddress();
                                    }
                                }, byteBuf.retain()))))
                .then(signal.asMono());
    }

    @Override
    public void start() {
        starter.doOnNext(d -> disposable = d)
                .subscribe();
    }

    @Override
    public void shutdown() {
        if (Objects.isNull(disposable)) {
            return;
        }

        disposable.dispose();
    }
}
