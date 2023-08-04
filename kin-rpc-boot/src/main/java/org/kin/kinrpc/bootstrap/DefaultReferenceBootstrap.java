package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.ExtensionException;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.cluster.invoker.ClusterInvoker;
import org.kin.kinrpc.cluster.invoker.ZoneAwareClusterInvoker;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryManager;
import org.kin.kinrpc.registry.directory.RegistryDirectory;
import org.kin.kinrpc.registry.directory.StaticDirectory;

import java.util.ArrayList;
import java.util.List;

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
            List<ReferenceInvoker<?>> clusterInvokers = new ArrayList<>();
            //获取注册中心client, 并订阅服务
            for (RegistryConfig registryConfig : config.getRegistries()) {
                Registry registry = RegistryManager.createRegistryIfAbsent(registryConfig);
                RegistryDirectory directory = new RegistryDirectory(config, registry);

                clusterInvokers.add(ExtensionLoader.getExtension(ClusterInvoker.class, config.getCluster(), config, registryConfig, directory));
            }
            if (clusterInvokers.size() > 1) {
                //多注册中心
                clusterInvoker = new ZoneAwareClusterInvoker<>(config, new StaticDirectory(clusterInvokers));
            } else {
                //单注册中心
                clusterInvoker = (ClusterInvoker<T>) clusterInvokers.get(0);
            }
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
