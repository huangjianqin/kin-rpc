package org.kin.kinrpc.cluster;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.loadbalance.LoadBalances;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.cluster.router.Routers;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.Registries;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.RPCProvider;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.serializer.Serializers;
import org.kin.kinrpc.rpc.transport.protocol.RPCHeartbeat;
import org.kin.transport.netty.core.protocol.ProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Clusters {
    private static final Logger log = LoggerFactory.getLogger(Clusters.class);
    private static final Cache<Integer, RPCProvider> PROVIDER_CACHE = CacheBuilder.newBuilder().build();
    private static final Cache<String, ClusterInvoker> REFERENCE_CACHE = CacheBuilder.newBuilder().build();
    private static final ThreadManager THREADS = ThreadManager.fix(2, "provider-unregister-executor");
    private static volatile boolean isStopped = false;
    private static final int HEARTBEAT_INTERVAL = 3;

    static {
        ProtocolFactory.init(RPCHeartbeat.class.getPackage().getName());
        JvmCloseCleaner.DEFAULT().add(() -> {
            for (RPCProvider provider : PROVIDER_CACHE.asMap().values()) {
                provider.shutdown();
                for (URL url : provider.getAvailableServices()) {
                    unRegisterService(url);
                }
            }
            for (ClusterInvoker clusterInvoker : REFERENCE_CACHE.asMap().values()) {
                clusterInvoker.close();
                Registries.closeRegistry(clusterInvoker.getUrl());
            }
            THREADS.shutdown();
        });
        //心跳检查, 每隔一定时间检查provider是否异常, 并取消服务注册
        THREADS.execute(() -> {
            while (!isStopped) {
                long sleepTime = HEARTBEAT_INTERVAL - TimeUtils.timestamp() % HEARTBEAT_INTERVAL;
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {

                }

                for (RPCProvider provider : new ArrayList<>(PROVIDER_CACHE.asMap().values())) {
                    if (!provider.isAlive()) {
                        int port = provider.getPort();
                        PROVIDER_CACHE.invalidate(port);
                        THREADS.execute(() -> {
                            provider.shutdown();
                            for (URL url : provider.getAvailableServices()) {
                                unRegisterService(url);
                            }
                        });
                    }
                }
            }
        });
    }

    private Clusters() {
    }

    public static synchronized void export(URL url, Class interfaceClass, Object instance) {
        String host = url.getHost();
        int port = url.getPort();
        String serializerType = url.getParam(Constants.SERIALIZE_KEY);
        boolean byteCodeInvoke = Boolean.parseBoolean(url.getParam(Constants.BYTE_CODE_INVOKE_KEY));
        Serializer serializer = Serializers.getSerializer(serializerType);
        Preconditions.checkNotNull(serializer, "unvalid serializer type: [" + serializerType + "]");
        RPCProvider provider;
        try {
            provider = PROVIDER_CACHE.get(port, () -> {
                RPCProvider provider0 = new RPCProvider(host, port, serializer, byteCodeInvoke, Boolean.parseBoolean(url.getParam(Constants.COMPRESSION_KEY)));
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

    private static void unRegisterService(URL url) {
        Registry registry = Registries.getRegistry(url);
        if (registry != null) {
            registry.unRegister(url.getServiceName(), NetUtils.getIp(), url.getPort());
            Registries.closeRegistry(url);
        }
    }

    public static synchronized void disableService(URL url, Class interfaceClass) {
        unRegisterService(url);

        RPCProvider provider = PROVIDER_CACHE.getIfPresent(url.getPort());
        if(Objects.nonNull(provider)){
            provider.disableService(url);
            provider.tell((p)-> {
                if (!p.isBusy()) {
                    //该端口没有提供服务, 关闭网络连接
                    p.shutdown();
                    PROVIDER_CACHE.invalidate(url.getPort());
                }
            });
        }
    }

    public static synchronized <T> T reference(URL url, Class<T> interfaceClass) {
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

    public static synchronized void disableReference(URL url) {
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

    public static synchronized void shutdownHeartBeat() {
        THREADS.shutdown();
    }
}
