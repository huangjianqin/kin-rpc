package org.kin.kinrpc.rpc.cluster;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.Registries;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.RPCProvider;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.kin.kinrpc.transport.ProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Clusters {
    private static final Logger log = LoggerFactory.getLogger("cluster");
    private static final Cache<Integer, RPCProvider> PROVIDER_CACHE = CacheBuilder.newBuilder().build();
    private static final Cache<String, ClusterInvoker> REFERENCE_CACHE = CacheBuilder.newBuilder().build();
    private static final ThreadManager threadManager = new ThreadManager(
            new ThreadPoolExecutor(0, 2, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new SimpleThreadFactory("provider-unregister-executor")));
    private static volatile boolean isStopped = false;
    private static final int HEARTBEAT_INTERVAL = 3;

    static {
        ProtocolFactory.init("org.kin.kinrpc.rpc");
        JvmCloseCleaner.DEFAULT().add(() -> {
            for(RPCProvider provider: PROVIDER_CACHE.asMap().values()){
                provider.shutdown();
                for (URL url : provider.getAvailableServices()) {
                    unRegisterService(url);
                }
            }
            for(ClusterInvoker clusterInvoker: REFERENCE_CACHE.asMap().values()){
                clusterInvoker.close();
                Registries.closeRegistry(clusterInvoker.getUrl());
            }
            threadManager.shutdown();
        });
        //心跳检查, 每隔一定时间检查provider是否异常, 并取消服务注册
        threadManager.execute(() -> {
            while(!isStopped){
                long sleepTime = HEARTBEAT_INTERVAL - TimeUtils.timestamp() % HEARTBEAT_INTERVAL;
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {

                }

                for(RPCProvider provider: new ArrayList<>(PROVIDER_CACHE.asMap().values())){
                    if(!provider.isAlive()){
                        int port = provider.getPort();
                        PROVIDER_CACHE.invalidate(port);
                        threadManager.execute(() -> {
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

    private Clusters(){
    }

    public static synchronized void export(URL url, Class interfaceClass, Object instance){
        int port = url.getPort();
        SerializerType serializerType = SerializerType.getByName(url.getParam(Constants.SERIALIZE_KEY));
        RPCProvider provider;
        try {
            provider = PROVIDER_CACHE.get(port, () -> {
                RPCProvider provider0 = new RPCProvider(port, serializerType.newInstance());
                provider0.start();

                return provider0;
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }

        try {
            provider.addService(url, interfaceClass, instance);
            Registry registry = Registries.getRegistry(url);
            if(registry != null){
                registry.register(url.getServiceName(), "0.0.0.0", port);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            disableService(url, interfaceClass);
        }
    }

    private static void unRegisterService(URL url){
        Registry registry = Registries.getRegistry(url);
        if(registry != null){
            registry.unRegister(url.getServiceName(), "0.0.0.0", url.getPort());
            Registries.closeRegistry(url);
        }
    }

    public static synchronized void disableService(URL url, Class interfaceClass){
        unRegisterService(url);

        RPCProvider provider = PROVIDER_CACHE.getIfPresent(url.getPort());
        provider.disableService(url);
        if(!provider.isBusy()){
            //该端口没有提供服务, 关闭网络连接
            provider.shutdown();
            PROVIDER_CACHE.invalidate(url.getPort());
        }
    }

    public static synchronized <T> T reference(URL url, Class<T> interfaceClass){
        Registry registry = Registries.getRegistry(url);
        Preconditions.checkNotNull(registry);

        //构建Cluster类
        int timeout = Integer.valueOf(url.getParam(Constants.TIMEOUT_KEY));
        int retryTimes = Integer.valueOf(url.getParam(Constants.RETRY_TIMES_KEY));
        int retryTimeout = Integer.valueOf(url.getParam(Constants.RETRY_TIMEOUT_KEY));
        Cluster cluster = new ClusterImpl(registry, url.getServiceName(), timeout);

        ClusterInvoker clusterInvoker = new ClusterInvoker(cluster, retryTimes, retryTimeout, url);

        Object jdkProxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, clusterInvoker);

        REFERENCE_CACHE.put(url.getServiceName(), clusterInvoker);

        return interfaceClass.cast(jdkProxy);
    }

    public static synchronized void disableReference(URL url){
        ClusterInvoker clusterInvoker = REFERENCE_CACHE.getIfPresent(url.getServiceName());

        if(clusterInvoker != null){
            clusterInvoker.close();
        }
        Registry registry = Registries.getRegistry(url);
        registry.unSubscribe(url.getServiceName());
        Registries.closeRegistry(url);

        REFERENCE_CACHE.invalidate(url.getServiceName());
    }

    public static synchronized void shutdownHeartBeat(){
        threadManager.shutdown();
    }
}
