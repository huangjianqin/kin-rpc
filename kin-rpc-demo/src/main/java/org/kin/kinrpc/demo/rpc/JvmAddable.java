package org.kin.kinrpc.demo.rpc;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.rpc.provider.AddableProvider;
import org.kin.kinrpc.demo.rpc.reference.AddableReference;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.transport.ProtocolType;

/**
 * @author huangjianqin
 * @date 2020/12/13
 */
public class JvmAddable {
    public static void main(String[] args) throws Exception {
        ServiceConfig<Addable> serviceConfig = AddableProvider.config();
        serviceConfig.protocol(ProtocolType.JVM);
        serviceConfig.export();

        ReferenceConfig<Addable> config = AddableReference.config();
        config.jvm();
        AddableReference.roundTest(config);
    }
}
