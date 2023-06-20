package org.kin.kinrpc.demo.rpc.grpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.conf.ProtocolType;
import org.kin.kinrpc.conf.ServiceConfig;

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
