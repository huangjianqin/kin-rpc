package org.kin.kinrpc.demo.rpc.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.rpc.service.Addable;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class KinRpcAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig<Addable> serviceConfig = AddableProvider.config();
        serviceConfig.exportSync();
    }
}
