package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.FilterChain;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.cluster.utils.ReferenceFilterUtils;
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
        ReferenceInvoker<T> referenceInvoker = protocol.refer(config, new JvmServiceInstance(config.getService()));

        //创建filter chain
        FilterChain<T> chain = FilterChain.create(ReferenceFilterUtils.internalPreFilters(),
                config.getFilters(),
                ReferenceFilterUtils.internalPostFilters(),
                referenceInvoker);

        return createProxy(chain);
    }

    @Override
    public void doUnRefer() {
        //do nothing
    }
}
