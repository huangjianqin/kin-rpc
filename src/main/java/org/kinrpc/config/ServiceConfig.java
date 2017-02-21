package org.kinrpc.config;

import org.apache.log4j.Logger;
import org.kinrpc.registry.Registry;
import org.kinrpc.registry.zookeeper.ZookeeperRegistry;
import org.kinrpc.remoting.transport.Server;
import org.kinrpc.rpc.future.ServiceFuture;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServiceConfig {
    private static final Logger log = Logger.getLogger(ServiceConfig.class);

    private ApplicationConfig applicationConfig;
    private ServerConfig serverConfig;
    private ZookeeperRegistryConfig registryConfig;

    private Object ref;
    private Class<?> interfaceClass;

    /**
     * 启动Server并添加服务
     * 注册服务,让消费者发现
     */
    public ServiceFuture export(){
        log.info("service exporting...");

        //Application配置
        if(this.applicationConfig == null){
            throw new IllegalStateException("provider must need to configure application");
        }

        //启动Server
        if(this.serverConfig == null){
            throw new IllegalStateException("provider must need to configure server");
        }

        Server server = serverConfig.getServer();
        server.addService(ref, interfaceClass);

        //注册中心实例化
        if(this.registryConfig == null){
            throw new IllegalStateException("provider must need to configure register");
        }
        ZookeeperRegistry zookeeperRegistry = registryConfig.getZookeeperRegistry();

        //注册服务
        try {
            zookeeperRegistry.connect();
            zookeeperRegistry.register(interfaceClass.getName(), serverConfig.getHost(), serverConfig.getPort());
        } catch (DataFormatException e) {
            log.error("registered service's address foramt error");
            e.printStackTrace();
        }

        return new ServiceFuture(this);
    }

    /**
     * 取消注册
     * 从Server中删除服务
     */
    public synchronized void disable(){
        log.info("service disable...");
        //取消注册
        ZookeeperRegistry zookeeperRegistry = registryConfig.getZookeeperRegistry();
        zookeeperRegistry.unRegister(interfaceClass.getName(), serverConfig.getHost(), serverConfig.getPort());
        registryConfig.closeRegistry();

        //从Server中删除服务
        Server server = serverConfig.getServer();
        server.disableService(interfaceClass.getName());
        server.shutdown();

        //释放ServiceFuture.sync的blocking
        this.notifyAll();
    }

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public void setApplicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public ZookeeperRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public void setRegistryConfig(ZookeeperRegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }
}
