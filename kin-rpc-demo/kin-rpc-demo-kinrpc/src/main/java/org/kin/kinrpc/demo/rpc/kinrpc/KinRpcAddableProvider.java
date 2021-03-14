package org.kin.kinrpc.demo.rpc.kinrpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.rpc.Addable;
import org.kin.kinrpc.demo.rpc.AddableProvider;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class KinRpcAddableProvider {
    public static void main(String[] args) throws Exception {
        ServiceConfig<Addable> serviceConfig = AddableProvider.config();
        serviceConfig.exportSync();

        JvmCloseCleaner.DEFAULT().add(serviceConfig::disable);
    }
}
