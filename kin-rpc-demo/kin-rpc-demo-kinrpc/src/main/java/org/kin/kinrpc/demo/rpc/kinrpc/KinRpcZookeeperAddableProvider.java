package org.kin.kinrpc.demo.rpc.kinrpc;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.config.ZookeeperRegistryConfig;
import org.kin.kinrpc.demo.rpc.Addable;
import org.kin.kinrpc.demo.rpc.Adder;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class KinRpcZookeeperAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig =
                Services.service(new Adder(), Addable.class)
                        .serviceName("test/Add")
                        .registry(ZookeeperRegistryConfig.create("127.0.0.1:2181").build());
        serviceConfig.exportSync();
    }
}
