package org.kinrpc.config;

import org.apache.log4j.Logger;
import org.kinrpc.common.Constants;
import org.kinrpc.registry.Registry;
import org.kinrpc.registry.zookeeper.ZookeeperRegistry;
import org.kinrpc.rpc.cluster.Cluster;
import org.kinrpc.rpc.cluster.DefaultCluster;
import org.kinrpc.rpc.invoker.AsyncInvoker;
import org.kinrpc.rpc.invoker.ClusterInvoker;

import java.lang.reflect.Proxy;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceConfig<T> {
    private static final Logger log = Logger.getLogger(ReferenceConfig.class);

    //配置
    private ApplicationConfig applicationConfig;
    private ZookeeperRegistryConfig registryConfig;
    private Class<T> interfaceClass;
    private int timeout = Constants.REFERENCE_DEFAULT_CONNECT_TIMEOUT;//默认5s

    public ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * 缓存一些变量
     */
    private ClusterInvoker clusterInvoker;
    private T service;

    /**
     * 检查配置参数正确性
     */
    private void checkConfig(){
        if(this.applicationConfig == null){
            throw new IllegalStateException("consumer must need to configure application");
        }

        if(this.registryConfig == null){
            throw new IllegalStateException("consumer must need to configure register");
        }

        if(this.interfaceClass == null){
            throw new IllegalStateException("consumer subscribed interface must be not ");
        }

        if(this.timeout < 0){
            throw new IllegalStateException("connection's timeout must greater than 0");
        }

    }

    public T get(){
        log.info("consumer getting service proxy...");

        //Application配置

        //注册中心实例化
        //连接注册中心
        ZookeeperRegistry zookeeperRegistry = registryConfig.getZookeeperRegistry();

        //构建Cluster类
        Cluster cluster = new DefaultCluster(zookeeperRegistry, interfaceClass, timeout);

        this.clusterInvoker = new ClusterInvoker(cluster);

        Object jdkProxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, clusterInvoker);

        this.service = interfaceClass.cast(jdkProxy);

        return service;
    }

    public synchronized T getService(){
        if(service != null){
            return service;
        }
        else{
            return get();
        }
    }

    public AsyncInvoker getAsyncInvoker(){
        if(this.clusterInvoker != null){
            return (AsyncInvoker)((ClusterInvoker)Proxy.getInvocationHandler(this.clusterInvoker));
        }
        return null;
    }

    /**
     * 不再使用服务了
     */
    public void disable(){
        //关闭底层cluster连接
        if(this.clusterInvoker != null){
            clusterInvoker.getCluster().shutdown();
        }

        //关闭注册中心
        registryConfig.closeRegistry();
    }

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public void setApplicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public ZookeeperRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public void setRegistryConfig(ZookeeperRegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
