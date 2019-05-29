package org.kin.kinrpc.api;

import org.kin.kinrpc.rpc.registry.zookeeper.ZookeeperRegistry;
import org.kin.kinrpc.remoting.transport.Server;
import org.kin.kinrpc.rpc.future.ServiceFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServiceConfig {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    //配置
    private ApplicationConfig applicationConfig;
    private ServerConfig serverConfig;
    private ZookeeperRegistryConfig registryConfig;
    private Object ref;
    private Class<?> interfaceClass;

    public ServiceConfig(Object ref, Class<?> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
    }

    /**
     * 检查配置参数正确性
     */
    private void checkConfig() {
        if (this.applicationConfig == null) {
            throw new IllegalStateException("provider must need to configure application");
        }

        if (this.serverConfig == null) {
            throw new IllegalStateException("provider must need to configure server");
        }

        if (this.registryConfig == null) {
            throw new IllegalStateException("provider must need to configure register");
        }

        if (this.ref == null) {
            throw new IllegalStateException("provider object must be not null");
        }

        if (this.interfaceClass == null) {
            throw new IllegalStateException("provider object interface must be not ");
        }

        //检查ref和interfaceClass的合法性,后面JavaProviderInvoker会再次检查
        if (!this.interfaceClass.isInterface()) {
            throw new IllegalStateException("the class '" + this.interfaceClass.getName() + "' is not a interface");
        }

        Class<?> serviceClass = this.ref.getClass();
        if (!this.interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalStateException("service class " + serviceClass.getName() + " must implements the certain interface '" + this.interfaceClass.getName() + "'");
        }
    }

    /**
     * 启动Server并添加服务
     * 注册服务,让消费者发现
     */
    public ServiceFuture export() {
        checkConfig();

        log.info("service exporting...");

        //Application配置

        //启动Server
        Server server = serverConfig.getServer();
        server.addService(ref, interfaceClass);

        //注册中心实例化
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
    public synchronized void disable() {
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

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }


    public ZookeeperRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public void setRegistryConfig(ZookeeperRegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }
}
