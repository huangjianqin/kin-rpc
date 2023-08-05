package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.Filter;
import org.kin.kinrpc.FilterChain;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.protocol.Protocol;
import org.kin.kinrpc.protocol.Protocols;
import org.kin.kinrpc.utils.ReferenceUtils;

import java.util.ArrayList;
import java.util.List;

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

        List<Filter> filters = new ArrayList<>(config.getFilters());
        filters.addAll(ReferenceUtils.getReferenceFilters());
        //创建filter chain
        FilterChain<T> chain = FilterChain.create(ReferenceUtils.internalPreFilters(),
                filters,
                ReferenceUtils.internalPostFilters(),
                referenceInvoker);

        return createProxy(chain);
    }

    @Override
    public void doUnRefer() {
        //do nothing
    }
}
