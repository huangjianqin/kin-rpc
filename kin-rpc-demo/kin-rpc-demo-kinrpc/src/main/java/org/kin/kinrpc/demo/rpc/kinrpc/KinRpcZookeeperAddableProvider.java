package org.kin.kinrpc.demo.rpc.kinrpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.conf.ServiceConfig;
import org.kin.kinrpc.conf.ZKRegistryConfig;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class KinRpcZookeeperAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig =
                Services.service(new Adder(), Addable.class)
                        .service("test/Add")
                        .registry(ZKRegistryConfig.create("127.0.0.1:2181").build());
        serviceConfig.exportSync();

        JvmCloseCleaner.instance().add(serviceConfig::disable);
    }
}
