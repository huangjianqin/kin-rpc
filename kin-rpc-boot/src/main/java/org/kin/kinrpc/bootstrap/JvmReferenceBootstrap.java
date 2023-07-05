package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.InterceptorChain;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.protocol.Protocol;
import org.kin.kinrpc.protocol.Protocols;

/**
 * reference boostrap for jvm
 *
 * @author huangjianqin
 * @date 2023/7/2
 */
public class JvmReferenceBootstrap<T> extends ProxyReferenceBootstrap<T> {
    public JvmReferenceBootstrap(ReferenceConfig<T> config) {
        super(config);
    }

    @Override
    public T doRefer() {
        //创建reference invoker
        Protocol protocol = Protocols.getByName(ProtocolType.JVM.getName());
        ReferenceInvoker<T> referenceInvoker = protocol.refer(new JvmServiceInstance(config.getService()), null);

        //创建interceptor chain
        InterceptorChain<T> chain = InterceptorChain.create(config, referenceInvoker);

        return createProxy(chain);
    }

    @Override
    public void doUnRefer() {
        //do nothing
    }
}
