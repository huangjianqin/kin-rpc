package org.kin.kinrpc.transport.grpc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.kin.kinrpc.AbstractProxyProtocol;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * 开发流程与grpc类似(编辑.proto, 并编译生成servcie base类), 只是底层通信和编程模式使用的是kinrpc
 *
 * @author huangjianqin
 * @date 2020/12/1
 */
public class GrpcProtocol extends AbstractProxyProtocol {
    private static final Cache<String, GrpcServer> servers = CacheBuilder.newBuilder().build();

    /**
     * @param proxyedInvoker grpc1.33, 开发者通过实现XXXGrpc.XXXImplBase, 本质上该类已实现了{@link BindableService}
     */
    @Override
    protected <T> Runnable doExport(T proxyedInvoker, Class<T> interfaceC, Url url) {
        String address = url.getAddress();
        //grpc server
        GrpcServer grpcServer;
        try {
            grpcServer = servers.get(address, () -> {
                GrpcHandlerRegistry registry = new GrpcHandlerRegistry();

                NettyServerBuilder builder =
                        NettyServerBuilder
                                .forPort(url.getPort())
                                .fallbackHandlerRegistry(registry);
                Server server = builder.build();
                return new GrpcServer(server, registry);
            });
        } catch (ExecutionException e) {
            throw new RpcCallErrorException(e);
        }

        grpcServer.registry.addService((BindableService) proxyedInvoker, url.getServiceKey());

        if (!grpcServer.started) {
            grpcServer.start();
        }

        return () -> grpcServer.registry.removeService(url.getServiceKey());
    }

    @Override
    protected <T> T doReference(Class<T> interfaceC, Url url) {
        return null;
    }

    @Override
    public void destroy() {

    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * grpc server数据封装
     */
    private class GrpcServer {
        /** grpc server */
        private final Server server;
        /** grpc 服务方法注册 */
        private final GrpcHandlerRegistry registry;
        /** server started标识 */
        private volatile boolean started;

        public GrpcServer(Server server, GrpcHandlerRegistry registry) {
            this.server = server;
            this.registry = registry;
        }

        /**
         * start
         */
        public void start() {
            try {
                started = true;
                server.start();
            } catch (IOException e) {
                throw new RpcCallErrorException(e);
            }
        }

        /**
         * shutdown
         */
        public void close() {
            //todo
            server.shutdown();
        }
    }
}
