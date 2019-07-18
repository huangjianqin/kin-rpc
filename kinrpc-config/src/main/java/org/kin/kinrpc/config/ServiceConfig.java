package org.kin.kinrpc.config;


import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServiceConfig extends AbstractConfig{
    //配置
    private ApplicationConfig applicationConfig = new ApplicationConfig();
    private ServerConfig serverConfig = ServerConfig.DEFAULT;
    private AbstractRegistryConfig registryConfig;
    private Object ref;
    private Class<?> interfaceClass;
    private String serviceName;
    private String serialize = Constants.KRYO_SERIALIZE;

    private URL url;
    private volatile boolean isExport;

    ServiceConfig(Object ref, Class<?> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
    }

    //---------------------------------------------------------------------------------------------------------
    @Override
    void check() {
        Preconditions.checkNotNull(this.ref, "provider object must be not null");
        Preconditions.checkNotNull(this.interfaceClass, "provider object interface must be not");

        //检查ref和interfaceClass的合法性
        if (!this.interfaceClass.isInterface()) {
            throw new IllegalStateException("the class '" + this.interfaceClass.getName() + "' is not a interface");
        }

        Class<?> serviceClass = this.ref.getClass();
        if (!this.interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalStateException("service class " + serviceClass.getName() + " must implements the certain interface '" + this.interfaceClass.getName() + "'");
        }

        this.applicationConfig.check();
        this.serverConfig.check();
        if(this.registryConfig != null){
            this.registryConfig.check();
        }
    }

    /**
     * 暴露服务
     */
    public void export() {
        if(!isExport){
            check();

            Map<String, String> params = new HashMap<>();
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.SERIALIZE_KEY, serialize);

            url = createURL(applicationConfig, "0.0.0.0:" + serverConfig.getPort(), registryConfig, params);
            Preconditions.checkNotNull(url);

            Clusters.export(url, interfaceClass, ref);

            isExport = true;
        }

    }

    /**
     * 暴露服务
     */
    public void exportSync() {
        if(!isExport){
            check();

            Map<String, String> params = new HashMap<>();
            params.put(Constants.SERVICE_NAME_KEY, serviceName);
            params.put(Constants.SERIALIZE_KEY, serialize);

            url = createURL(applicationConfig, "0.0.0.0:" + serverConfig.getPort(), registryConfig, params);
            Preconditions.checkNotNull(url);

            Clusters.export(url, interfaceClass, ref);

            isExport = true;
        }

        while (true) {

        }
    }

    /**
     * 取消服务注册
     */
    public void disable() {
        if(isExport){
            Clusters.disableService(url, interfaceClass);
        }
    }

    //---------------------------------------builder------------------------------------------------------------
    public ServiceConfig appName(String appName) {
        if(!isExport){
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ServiceConfig bind(int port){
        if(!isExport){
            this.serverConfig = new ServerConfig(port);
        }
        return this;
    }

    public ServiceConfig urls(String... urls){
        if(!isExport){
            this.registryConfig = new DirectURLsRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ServiceConfig zookeeper(String address){
        if(!isExport){
            this.registryConfig = new ZookeeperRegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig zookeeper2(String address){
        if(!isExport){
            this.registryConfig = new Zookeeper2RegistryConfig(address);
        }
        return this;
    }

    public ServiceConfig registrySessionTimeout(int sessionTimeout){
        if(!isExport){
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ServiceConfig serviceName(String serviceName){
        if(!isExport){
            this.serviceName = serviceName;
        }
        return this;
    }

    public ServiceConfig serialize(String serialize){
        if(!isExport){
            this.serialize = serialize;
        }
        return this;
    }

    //setter && getter
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public AbstractRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Object getRef() {
        return ref;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }
}
