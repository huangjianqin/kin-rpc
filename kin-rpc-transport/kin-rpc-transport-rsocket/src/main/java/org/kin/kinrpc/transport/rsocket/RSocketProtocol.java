package org.kin.kinrpc.transport.rsocket;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.ipc.RoutingServerRSocket;
import io.rsocket.rpc.AbstractRSocketService;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.transport.AbstractProxyProtocol;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2021/1/30
 */
public class RSocketProtocol extends AbstractProxyProtocol {
    private static final Cache<String, RSocketServer> SERVERS =
            CacheBuilder.newBuilder()
                    .removalListener(n -> ((RSocketServer) n.getValue()).close()).build();

    @Override
    protected <T> Runnable doExport(T proxyedInvoker, Class<T> interfaceC, Url url) {
        String address = url.getAddress();
        //rsocket rpc中定义的服务名
        String rsocketServiceName = interfaceC.getName();
        //rsocket server
        RSocketServer rsocketServer;
        synchronized (SERVERS) {
            try {
                rsocketServer = SERVERS.get(address, () -> {
                    CopyOnWriteRouter router = new CopyOnWriteRouter();
                    RoutingServerRSocket routingServerRSocket = new RoutingServerRSocket(router);
                    CloseableChannel closeableChannel = io.rsocket.core.RSocketServer.create()
                            .payloadDecoder(PayloadDecoder.ZERO_COPY)
                            .acceptor((setup, sendingRSocket) -> Mono.just(routingServerRSocket))
                            .bind(TcpServerTransport.create(url.getHost(), url.getPort())).block();
                    return new RSocketServer(address, closeableChannel, router);
                });
            } catch (ExecutionException e) {
                throw new RpcCallErrorException(e);
            }

            /**
             * 创建{@link io.rsocket.rpc.AbstractRSocketService}实例
             * rsocket rpc自动生成代码中, {@link io.rsocket.rpc.AbstractRSocketService}实现类类名=服务接口全限定类名+Server
             */
            String rsocketServiceImplClassName = rsocketServiceName.concat("Server");
            AbstractRSocketService rSocketService;
            try {
                Class<?> rsocketServiceImplClass = Class.forName(rsocketServiceImplClassName);
                //rsocket rpc自动生成代码中只有一个构造器
                rSocketService = (AbstractRSocketService) rsocketServiceImplClass.getConstructors()[0].newInstance(
                        proxyedInvoker, Optional.empty(), Optional.empty(), Optional.empty());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(String.format("Failed to find class '%s', please make sure your code " +
                        "was generated by the rsocket-rpc-protobuf-compiler.", rsocketServiceImplClassName), e);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to new '%s' instance", e);
            }

            //注册route
            rSocketService.selfRegister(rsocketServer.router);
        }

        info("rsocket service '{}' export address '{}'", url.getServiceName(), url.getAddress());

        return () -> {
            rsocketServer.router.unregisterFireAndForgetRoute(rsocketServiceName);
            rsocketServer.router.unregisterRequestResponseRoute(rsocketServiceName);
            rsocketServer.router.unregisterRequestStreamRoute(rsocketServiceName);
            rsocketServer.router.unregisterRequestChannelRoute(rsocketServiceName);

            synchronized (SERVERS) {
                rsocketServer.close();
            }
        };
    }

    @Override
    protected <T> T doReference(Class<T> interfaceC, Url url) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> AsyncInvoker<T> reference(Url url) throws Throwable {
        Class<T> interfaceC;
        try {
            interfaceC = (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw e;
        }

        boolean isAsyncCall = url.getBooleanParam(Constants.ASYNC_KEY);
        if (isAsyncCall) {
            //打印警告
            //使用rsocket时, 推荐使用同步rpc call, 因为异步rpc call会使得服务接口返回空值为null, 而且根据reactive的定义, mono flux本身已实现异步操作, 故无需多此一举
            warn("recommand to use sync call when using 'roscket' protocol!!!! because async call service method call will return null and roscket implement has included async operation itself");
        }

        //rsocket rpc中定义的服务名
        String rsocketServiceName = interfaceC.getName();
        //client
        RSocket rSocket = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .connect(TcpClientTransport.create(url.getHost(), url.getPort()))
                .retry(3)
                .block();

        Preconditions.checkNotNull(rSocket, "construct rsocket client error");

        /**
         * rsocket rpc自动生成代码中, rpc client实现类类名=服务接口全限定类名+Client
         */
        String rsocketServiceClientClassName = rsocketServiceName.concat("Client");
        T rSocketServiceClient;
        try {
            Class<?> rSocketServiceClientClass = Class.forName(rsocketServiceClientClassName);
            //构建rsocket service client
            rSocketServiceClient = (T) rSocketServiceClientClass.getConstructor(RSocket.class).newInstance(rSocket);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Failed to find class '%s', please make sure your code " +
                    "was generated by the rsocket-rpc-protobuf-compiler.", rsocketServiceClientClassName), e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to new '%s' instance", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to '%s' constructor with params " + RSocket.class.getName(), e);
        }

        info("rsocket reference '{}' refer address '{}'", url.getServiceName(), url.getAddress());

        return generateAsyncInvoker(
                url,
                interfaceC,
                rSocketServiceClient,
                url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY),
                rSocket::dispose);
    }

    @Override
    public void destroy() {
        SERVERS.invalidateAll();
    }

    //------------------------------------------------------------------------------------------------------------
    private static class RSocketServer {
        /** server池的key */
        private final String cacheKey;
        /** 控制server close */
        private final CloseableChannel closeableChannel;
        /** router */
        private final CopyOnWriteRouter router;

        public RSocketServer(String cacheKey, CloseableChannel closeableChannel, CopyOnWriteRouter router) {
            this.cacheKey = cacheKey;
            this.closeableChannel = closeableChannel;
            this.router = router;
        }

        /**
         * close server
         */
        private void close() {
            if (router.isEmpty()) {
                closeableChannel.dispose();
                SERVERS.invalidate(cacheKey);
            }
        }
    }
}
