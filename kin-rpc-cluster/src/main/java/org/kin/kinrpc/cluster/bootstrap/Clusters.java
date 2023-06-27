package org.kin.kinrpc.cluster.bootstrap;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Clusters {
    private static final Logger log = LoggerFactory.getLogger(Clusters.class);

    private static final Cache<String, ClusterInvoker1> REFERENCE_CACHE = CacheBuilder.newBuilder().build();
    private static final Cache<Url, Exporter> EXPORTER_CACHE = CacheBuilder.newBuilder().build();

    private Clusters() {
    }

    /**
     * 暴露服务
     */
    public static synchronized <T> void export(Url url, Class<T> interfaceClass, T instance) {
        String protocolName = url.getProtocol();
        Protocol protocol = ExtensionLoader.getExtension(Protocol.class, protocolName);

        Preconditions.checkNotNull(protocol, String.format("unknown protocol: %s", protocolName));

        boolean byteCodeInvoke = url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY);
        ProviderInvoker<T> invoker;
        if (byteCodeInvoke) {
            invoker = new JavassistProviderInvoker<>(url, instance, interfaceClass);
        } else {
            invoker = new JdkProxyProviderInvoker<>(url, instance, interfaceClass);
        }

        //先启动服务
        Exporter<T> export = null;
        try {
            export = protocol.export(invoker);
        } catch (Throwable throwable) {
            ExceptionUtils.throwExt(throwable);
        }

        EXPORTER_CACHE.put(url, export);

        //再注册
        Registry registry = RegistryHelper.getRegistry(url);
        if (registry != null) {
            registry.register(url);
        }
    }

    /**
     * 取消服务注册
     */
    private static void unRegisterService(Url url) {
        Registry registry = RegistryHelper.getRegistry(url);
        if (registry != null) {
            registry.unregister(url);
            RegistryHelper.closeRegistry(url);
        }
    }

    /**
     * 停用服务
     */
    public static synchronized void disableService(Url url) {
        String protocolName = url.getProtocol();
        Protocol protocol = ExtensionLoader.getExtension(Protocol.class, protocolName);

        Preconditions.checkNotNull(protocol, String.format("unknown protocol: %s", protocolName));

        //先取消注册
        unRegisterService(url);
        //再关闭服务
        Exporter<?> exporter = EXPORTER_CACHE.getIfPresent(url);
        if (Objects.nonNull(exporter)) {
            exporter.unexport();
        }
    }

    /**
     * 引用服务
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T reference(Url url, Class<T> interfaceClass, List<Notifier<?>> notifiers) {
        Registry registry = RegistryHelper.getRegistry(url);
        Preconditions.checkNotNull(registry);

        //构建Cluster类
        String loadBalanceType = url.getParam(Constants.LOADBALANCE_KEY);
        LoadBalance loadBalance = ExtensionLoader.getExtension(LoadBalance.class, loadBalanceType);
        String routerType = url.getParam(Constants.ROUTER_KEY);
        Router router = ExtensionLoader.getExtension(Router.class, routerType);

        Preconditions.checkNotNull(loadBalance, "unvalid loadbalance type: [" + loadBalanceType + "]");
        Preconditions.checkNotNull(router, "unvalid router type: [" + routerType + "]");

        Cluster<T> cluster = new ClusterImpl<>(registry, url, router, loadBalance);

        T proxy;

        boolean byteCodeInvoke = url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY);
        if (byteCodeInvoke) {
            JavassistClusterInvoker<T> javassistClusterInvoker = new JavassistClusterInvoker<>(cluster, url, interfaceClass, notifiers);
            proxy = javassistClusterInvoker.proxy();

            REFERENCE_CACHE.put(url.getServiceKey(), javassistClusterInvoker);
        } else {
            JdkProxyClusterInvoker<T> jdkProxyClusterInvoker = new JdkProxyClusterInvoker<>(cluster, url, notifiers);
            proxy = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, jdkProxyClusterInvoker);

            REFERENCE_CACHE.put(url.getServiceKey(), jdkProxyClusterInvoker);
        }

        return proxy;
    }

    /**
     * close 引用服务
     */
    public static synchronized void disableReference(Url url) {
        ClusterInvoker1<?> clusterInvoker = REFERENCE_CACHE.getIfPresent(url.getServiceKey());

        if (clusterInvoker != null) {
            clusterInvoker.close();
        }
        Registry registry = RegistryHelper.getRegistry(url);
        if (Objects.nonNull(registry)) {
            registry.unsubscribe(url.getServiceKey());
            RegistryHelper.closeRegistry(url);
        }

        REFERENCE_CACHE.invalidate(url.getServiceKey());
    }
}
