package org.kin.kinrpc.demo.rpc.rsocket;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.transport.ProtocolType;

/**
 * @author huangjianqin
 * @date 2021/1/31
 */
public class RSocketServiceProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig<RSocketService> serviceConfig =
                Services.service(new RSocketServiceImpl(), RSocketService.class)
                        .version("001")
                        .actorLike();
        serviceConfig.protocol(ProtocolType.RSOCKET);
        serviceConfig.exportSync();

        JvmCloseCleaner.instance().add(serviceConfig::disable);
    }
}
