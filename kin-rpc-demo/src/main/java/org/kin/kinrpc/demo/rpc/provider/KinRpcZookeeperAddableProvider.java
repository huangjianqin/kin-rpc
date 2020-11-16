package org.kin.kinrpc.demo.rpc.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.demo.rpc.service.Adder;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class KinRpcZookeeperAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = Services.service(new Adder(), Addable.class).serviceName("test/Add").zookeeper("127.0.0.1:2181");
        serviceConfig.exportSync();
    }
}
