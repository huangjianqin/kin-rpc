package org.kin.kinrpc.config.test.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.config.test.service.Addable;
import org.kin.kinrpc.config.test.service.Adder;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class AddableProvider {
    public static void main(String[] args) throws InterruptedException {
        ServiceConfig serviceConfig = Services.service(new Adder(), Addable.class);
        serviceConfig.export();
        while(true){

        }
//        serviceConfig.disable();
    }
}
