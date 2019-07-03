package org.kin.kinrpc.demo.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.demo.service.Addable;
import org.kin.kinrpc.demo.service.Adder;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class Zookeeper2AddableProvider {
    public static void main(String[] args) {
        ServiceConfig serviceConfig = Services.service(new Adder(), Addable.class).serviceName("test/Add").zookeeper2("127.0.0.1:2181");
        serviceConfig.exportSync();
    }
}
