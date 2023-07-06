package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.FilterChain;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.cluster.invoker.ClusterInvoker;
import org.kin.kinrpc.cluster.invoker.RpcCallInvoker;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.directory.CompositeDirectory;
import org.kin.kinrpc.registry.directory.Directory;

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
        //创建filter chain
        FilterChain<T> chain = FilterChain.create(config, (Invoker<T>) RpcCallInvoker.INSTANCE);

        //创建loadbalance
        LoadBalance loadBalance = ExtensionLoader.getExtension(LoadBalance.class, config.getLoadBalance());
        //创建router
        Router router = ExtensionLoader.getExtension(Router.class, config.getRouter());

        //获取注册中心client, 并订阅服务
        List<Directory> directories = new ArrayList<>();
        for (RegistryConfig registryConfig : config.getRegistries()) {
            Registry registry = RegistryHelper.getRegistry(registryConfig);
            directories.add(registry.subscribe(config));
        }

        clusterInvoker = ExtensionLoader.getExtension(ClusterInvoker.class, config.getCluster(),
                config, new CompositeDirectory(directories),
                router, loadBalance, chain);

        return createProxy(clusterInvoker);
    }

    @Override
    public void doUnRefer() {
        clusterInvoker.destroy();

        //获取注册中心client, 并取消订阅服务
        String service = config.getService();
        for (RegistryConfig registryConfig : config.getRegistries()) {
            Registry registry = RegistryHelper.getRegistry(registryConfig);
            registry.unsubscribe(service);
        }
    }
}
