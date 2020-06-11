package org.kin.kinrpc.rpc.demo.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.rpc.demo.service.Addable;
import org.kin.kinrpc.rpc.demo.service.Adder;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class ZookeeperAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = Services.service(new Adder(), Addable.class).serviceName("test/Add").zookeeper("127.0.0.1:2181");
        serviceConfig.exportSync();
    }
}
