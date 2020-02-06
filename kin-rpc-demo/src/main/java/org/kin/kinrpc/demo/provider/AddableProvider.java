package org.kin.kinrpc.demo.provider;

import org.kin.kinrpc.config.SerializerType;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.demo.service.Addable;
import org.kin.kinrpc.demo.service.Adder;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class AddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig serviceConfig = Services.service(new Adder(), Addable.class).serialize(SerializerType.JSON.getType()).version("001").compress().singleThread();
        serviceConfig.exportSync();
    }
}
