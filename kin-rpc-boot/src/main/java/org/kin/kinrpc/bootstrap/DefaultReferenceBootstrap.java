package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.cluster.invoker.ClusterInvoker;
import org.kin.kinrpc.config.ReferenceConfig;

/**
 * default boostrap reference
 *
 * @author huangjianqin
 * @date 2023/6/30
 */
public class DefaultReferenceBootstrap<T> extends ProxyReferenceBootstrap<T> {
    /** cluster invoker */
    private volatile ClusterInvoker<T> clusterInvoker;

    public DefaultReferenceBootstrap(ReferenceConfig<T> config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T doRefer() {
        clusterInvoker = ExtensionLoader.getExtension(ClusterInvoker.class, config.getCluster(), config);

        return createProxy(clusterInvoker);
    }

    @Override
    public void doUnRefer() {
        clusterInvoker.destroy();
    }
}
