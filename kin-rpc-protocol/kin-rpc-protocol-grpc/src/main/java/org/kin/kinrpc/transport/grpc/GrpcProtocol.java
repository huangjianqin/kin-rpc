package org.kin.kinrpc.transport.grpc;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.MethodMetadata;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020/12/1
 */
@Extension("grpc")
public final class GrpcProtocol extends AbstractProtocol {

    @Override
    protected RemotingServer createServer(ServerConfig serverConfig, @Nullable ManagedExecutor executor) {
        return new GrpcServer(serverConfig.getHost(), serverConfig.getPort(), executor, serverConfig.getSsl());
    }

    @Override
    protected void onExport(RpcService<?> service, RemotingServer server) {
        GrpcServer grpcServer = (GrpcServer) server;
        //add grpc service descriptor
        List<Integer> handlerIds = service.getMethodMetadatas()
                .stream()
                .map(MethodMetadata::handlerId)
                .collect(Collectors.toList());
        grpcServer.registerService(service.serviceId(), handlerIds);
    }

    @Override
    protected void onUnExport(RpcService<?> service, RemotingServer server) {
        GrpcServer grpcServer = (GrpcServer) server;
        //remove grpc service descriptor
        grpcServer.unregisterService(service.serviceId());
    }

    @Override
    protected RemotingClient createClient(ServiceInstance instance, SslConfig sslConfig) {
        return new GrpcClient(instance.host(), instance.port(), sslConfig);
    }
}
