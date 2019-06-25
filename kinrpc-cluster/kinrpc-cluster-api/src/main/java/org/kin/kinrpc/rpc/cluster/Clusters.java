package org.kin.kinrpc.rpc.cluster;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.Registries;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.domain.RPCProvider;

import java.lang.reflect.Proxy;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Clusters {
    private static final Cache<Integer, RPCProvider> providerCache = CacheBuilder.newBuilder().build();
    private static final Cache<String, ClusterInvoker> referenceCache = CacheBuilder.newBuilder().build();

    static {
        JvmCloseCleaner.DEFAULT().add(() -> {
            for(RPCProvider provider: providerCache.asMap().values()){
                provider.shutdown();
            }
        });
    }

    private void Clusters(){
    }

    public static synchronized void export(URL url, Class interfaceClass, Object instance){
        int port = url.getPort();
        RPCProvider provider = null;
        try {
            provider = providerCache.get(port, () -> {
                RPCProvider provider0 = new RPCProvider(port);
                provider0.start();

                return provider0;
            });
        } catch (Exception e) {
            ExceptionUtils.log(e);
            return;
        }

        try {
            Registry registry = Registries.getRegistry(url);
            Preconditions.checkNotNull(registry);
            provider.addService(url.getServiceName(), interfaceClass, instance);
            registry.register(interfaceClass.getName(), "0.0.0.0", port);
        } catch (Exception e) {
            ExceptionUtils.log(e);
            disableService(url, interfaceClass);
        }
    }

    public static synchronized void disableService(URL url, Class interfaceClass){
        Registry registry = Registries.getRegistry(url);
        registry.unRegister(interfaceClass.getName(), "0.0.0.0", url.getPort());
        Registries.closeRegistry(url);

        RPCProvider provider = providerCache.getIfPresent(url.getPort());
        provider.disableService(interfaceClass.getName());
        if(!provider.isBusy()){
            //该端口没有提供服务, 关闭网络连接
            provider.shutdown();
            providerCache.invalidate(url.getPort());
        }
    }

    public static synchronized <T> T reference(URL url, Class<T> interfaceClass){
        Registry registry = Registries.getRegistry(url);
        Preconditions.checkNotNull(registry);

        //构建Cluster类
        int timeout = Integer.valueOf(url.getParam(Constants.TIMEOUT));
        Cluster cluster = new DefaultCluster(registry, url.getServiceName(), timeout);

        ClusterInvoker clusterInvoker = new ClusterInvoker(cluster);

        Object jdkProxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, clusterInvoker);

        referenceCache.put(url.getServiceName(), clusterInvoker);

        return interfaceClass.cast(jdkProxy);
    }

    public static synchronized void disableReference(URL url){
        ClusterInvoker clusterInvoker = referenceCache.getIfPresent(url.getServiceName());

        clusterInvoker.close();
        Registries.closeRegistry(url);

        referenceCache.invalidate(url.getServiceName());
    }
}
