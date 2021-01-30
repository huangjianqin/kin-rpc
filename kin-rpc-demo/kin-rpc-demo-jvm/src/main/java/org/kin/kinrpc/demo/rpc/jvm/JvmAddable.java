package org.kin.kinrpc.demo.rpc.jvm;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.rpc.Addable;
import org.kin.kinrpc.demo.rpc.AddableProvider;
import org.kin.kinrpc.demo.rpc.AddableReference;
import org.kin.kinrpc.transport.ProtocolType;

/**
 * @author huangjianqin
 * @date 2020/12/13
 */
public class JvmAddable {
    public static void main(String[] args) throws Exception {
        ServiceConfig<Addable> serviceConfig = AddableProvider.config();
        serviceConfig.protocol(ProtocolType.Jvm);
        serviceConfig.export();

        ReferenceConfig<Addable> config = AddableReference.config();
        config.jvm();
        AddableReference.roundTest(config);
    }
}
