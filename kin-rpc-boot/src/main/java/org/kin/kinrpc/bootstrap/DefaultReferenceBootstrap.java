package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.InterceptorChain;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.KinRpcAppContext;
import org.kin.kinrpc.cluster.ReferenceProxy;
import org.kin.kinrpc.cluster.invoker.ClusterInvoker;
import org.kin.kinrpc.cluster.invoker.RpcCallInvoker;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.cluster.utils.ByteBuddyUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.directory.CompositeDirectory;
import org.kin.kinrpc.registry.directory.Directory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/30
 */
public class DefaultReferenceBootstrap<T> extends ReferenceBootstrap<T> {
    /** service reference */
    private volatile T reference;

    public DefaultReferenceBootstrap(ReferenceConfig<T> config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T doRefer() {
        if (Objects.nonNull(reference)) {
            return reference;
        }

        //创建interceptor chain
        InterceptorChain<T> chain = InterceptorChain.create(config, (Invoker<T>) RpcCallInvoker.INSTANCE);

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

        ClusterInvoker<T> clusterInvoker = ExtensionLoader.getExtension(ClusterInvoker.class, config.getCluster(),
                config, new CompositeDirectory(directories),
                router, loadBalance, chain);
        ReferenceProxy referenceProxy = new ReferenceProxy(config, clusterInvoker);

        Class<T> interfaceClass = config.getInterfaceClass();
        if (KinRpcAppContext.ENHANCE) {
            reference = ByteBuddyUtils.build(interfaceClass, referenceProxy);
        } else {
            reference = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                    new Class[]{interfaceClass},
                    referenceProxy);
        }

        return reference;
    }

    @Override
    public void doUnRefer() {
        if (Objects.isNull(reference)) {
            return;
        }

        //获取注册中心client, 并取消订阅服务
        String service = config.service();
        for (RegistryConfig registryConfig : config.getRegistries()) {
            Registry registry = RegistryHelper.getRegistry(registryConfig);
            registry.unsubscribe(service);
        }

        //释放引用
        reference = null;
    }
}
