package org.kin.kinrpc.demo.rpc.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.transport.ProtocolType;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class HttpAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig<Addable> serviceConfig = AddableProvider.config();
        serviceConfig.protocol(ProtocolType.Http);
        serviceConfig.exportSync();
    }
}
