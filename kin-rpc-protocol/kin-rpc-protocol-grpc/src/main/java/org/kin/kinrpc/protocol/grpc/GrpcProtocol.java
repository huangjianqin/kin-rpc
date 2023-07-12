package org.kin.kinrpc.protocol.grpc;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.MethodMetadata;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.grpc.GrpcServer;

import java.util.List;
import java.util.stream.Collectors;

import static org.kin.kinrpc.protocol.grpc.GrpcProtocol.NAME;

/**
 * @author huangjianqin
 * @date 2020/12/1
 */
@Extension(NAME)
public final class GrpcProtocol extends AbstractProtocol {
    /** 协议名 */
    public static final String NAME = "grpc";

    @Override
    protected String name() {
        return NAME;
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
}
