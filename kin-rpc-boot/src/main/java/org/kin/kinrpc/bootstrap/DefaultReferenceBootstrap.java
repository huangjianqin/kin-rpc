package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.ExtensionException;
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
        try {
            clusterInvoker = ExtensionLoader.getExtension(ClusterInvoker.class, config.getCluster(), config);
        } catch (Exception e) {
            throw new ExtensionException(String.format("can not create cluster named '%s', please check whether related SPI config is missing", config.getCluster()), e);
        }

        return createProxy(clusterInvoker);
    }

    @Override
    public void doUnRefer() {
        clusterInvoker.destroy();
    }
}
