package org.kin.kinrpc.cluster;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.actor.PinnedThreadSafeFuturesManager;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.loadbalance.LoadBalances;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.cluster.router.Routers;
import org.kin.kinrpc.registry.Registries;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.RpcProvider;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.RpcEndpointRefHandler;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.transport.netty.core.protocol.ProtocolFactory;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Clusters {
    private static final Logger log = LoggerFactory.getLogger(Clusters.class);
    private static final Cache<Integer, RpcProvider> PROVIDER_CACHE = CacheBuilder.newBuilder().build();
    private static final Cache<String, ClusterInvoker> REFERENCE_CACHE = CacheBuilder.newBuilder().build();

    static {
        ProtocolFactory.init(RpcRequestProtocol.class.getPackage().getName());
        JvmCloseCleaner.DEFAULT().add(Clusters::shutdown);
    }

    private Clusters() {
    }

    public static synchronized void export(Url url, Class interfaceClass, Object instance) {
        String host = url.getHost();
        int port = url.getPort();
        String serializerType = url.getParam(Constants.SERIALIZE_KEY);
        boolean byteCodeInvoke = Boolean.parseBoolean(url.getParam(Constants.BYTE_CODE_INVOKE_KEY));
        Serializer serializer = Serializers.getSerializer(serializerType);
        Preconditions.checkNotNull(serializer, "unvalid serializer type: [" + serializerType + "]");
        RpcProvider provider;
        try {
            provider = PROVIDER_CACHE.get(port, () -> {
                RpcProvider provider0 = new RpcProvider(host, port, serializer, byteCodeInvoke, Boolean.parseBoolean(url.getParam(Constants.COMPRESSION_KEY)));
                try {
                    provider0.start();
                } catch (Exception e) {
                    provider0.shutdownNow();
                    throw e;
                }

                return provider0;
            });
        } catch (UncheckedExecutionException uee) {
            throw (RuntimeException) uee.getCause();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }

        try {
            provider.addService(url, interfaceClass, instance);
            Registry registry = Registries.getRegistry(url);
            if (registry != null) {
                registry.register(url.getServiceName(), NetUtils.getIp(), port);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            disableService(url, interfaceClass);
        }
    }

    private static void unRegisterService(Url url) {
        Registry registry = Registries.getRegistry(url);
        if (registry != null) {
            registry.unRegister(url.getServiceName(), NetUtils.getIp(), url.getPort());
            Registries.closeRegistry(url);
        }
    }

    public static synchronized void disableService(Url url, Class interfaceClass) {
        unRegisterService(url);

        RpcProvider provider = PROVIDER_CACHE.getIfPresent(url.getPort());
        if (Objects.nonNull(provider)) {
            provider.disableService(url);
            provider.handle((p) -> {
                if (!p.isBusy()) {
                    //该端口没有提供服务, 关闭网络连接
                    p.shutdown();
                    PROVIDER_CACHE.invalidate(url.getPort());
                }
            });
        }
    }

    public static synchronized <T> T reference(Url url, Class<T> interfaceClass) {
        Registry registry = Registries.getRegistry(url);
        Preconditions.checkNotNull(registry);

        //构建Cluster类
        int timeout = Integer.parseInt(url.getParam(Constants.TIMEOUT_KEY));
        int retryTimes = Integer.parseInt(url.getParam(Constants.RETRY_TIMES_KEY));
        int retryTimeout = Integer.parseInt(url.getParam(Constants.RETRY_TIMEOUT_KEY));
        String loadBalanceType = url.getParam(Constants.LOADBALANCE_KEY);
        LoadBalance loadBalance = LoadBalances.getLoadBalance(loadBalanceType);
        String routerType = url.getParam(Constants.ROUTER_KEY);
        Router router = Routers.getRouter(routerType);

        Preconditions.checkNotNull(loadBalance, "unvalid loadbalance type: [" + loadBalanceType + "]");
        Preconditions.checkNotNull(router, "unvalid router type: [" + routerType + "]");

        Cluster cluster = new ClusterImpl(registry, url.getServiceName(), timeout, router, loadBalance);

        T proxy;

        boolean byteCodeInvoke = Boolean.parseBoolean(url.getParam(Constants.BYTE_CODE_INVOKE_KEY));
        if (byteCodeInvoke) {
            JavassistClusterInvoker<T> javassistClusterInvoker = new JavassistClusterInvoker<>(cluster, retryTimes, retryTimeout, url, interfaceClass);
            proxy = javassistClusterInvoker.proxy();

            REFERENCE_CACHE.put(url.getServiceName(), javassistClusterInvoker);
        } else {
            ReflectClusterInvoker reflectClusterInvoker = new ReflectClusterInvoker(cluster, retryTimes, retryTimeout, url);
            proxy = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, reflectClusterInvoker);

            REFERENCE_CACHE.put(url.getServiceName(), reflectClusterInvoker);
        }

        return proxy;
    }

    public static synchronized void disableReference(Url url) {
        ClusterInvoker clusterInvoker = REFERENCE_CACHE.getIfPresent(url.getServiceName());

        if (clusterInvoker != null) {
            clusterInvoker.close();
        }
        Registry registry = Registries.getRegistry(url);
        if (Objects.nonNull(registry)) {
            registry.unSubscribe(url.getServiceName());
            Registries.closeRegistry(url);
        }

        REFERENCE_CACHE.invalidate(url.getServiceName());
    }

    public static synchronized void shutdown() {
        for (RpcProvider provider : PROVIDER_CACHE.asMap().values()) {
            provider.shutdown();
            for (Url url : provider.getAvailableServices()) {
                unRegisterService(url);
            }
        }
        for (ClusterInvoker clusterInvoker : REFERENCE_CACHE.asMap().values()) {
            clusterInvoker.close();
            Registries.closeRegistry(clusterInvoker.getUrl());
        }
        //没有任何RPC 实例运行中
        RpcThreadPool.PROVIDER_WORKER.shutdown();
        RpcThreadPool.EXECUTORS.shutdown();
        RpcEndpointRefHandler.RECONNECT_EXECUTORS.shutdown();
        PinnedThreadSafeFuturesManager.instance().close();
        InOutBoundStatisicService.instance().close();
    }
}
