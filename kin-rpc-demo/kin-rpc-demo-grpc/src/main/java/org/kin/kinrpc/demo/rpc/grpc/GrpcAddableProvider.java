package org.kin.kinrpc.demo.rpc.grpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.ProtocolType;

/**
 * @author huangjianqin
 * @date 2020/12/2
 */
public class GrpcAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig<KinRpcGrpcServiceGrpc.GrpcService> serviceConfig =
                Services.service(new GrpcServiceImpl(), KinRpcGrpcServiceGrpc.GrpcService.class)
                        .version("001")
                        .actorLike();
        serviceConfig.protocol(ProtocolType.GRPC);
        serviceConfig.exportSync();

        JvmCloseCleaner.instance().add(serviceConfig::disable);
    }
}
