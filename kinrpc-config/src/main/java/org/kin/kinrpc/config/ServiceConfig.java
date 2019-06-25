package org.kin.kinrpc.config;


import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.cluster.Clusters;

/**
 * Created by 健勤 on 2017/2/12.
 */
class ServiceConfig extends Config{
    //配置
    private ApplicationConfig applicationConfig = new ApplicationConfig();
    private ServerConfig serverConfig = new ServerConfig(Constants.SERVER_DEFAULT_PORT);
    private RegistryConfig registryConfig;
    private Object ref;
    private Class<?> interfaceClass;
    private String serviceName;

    private URL url;
    private volatile boolean isExport;

    private ServiceConfig() {

    }

    public static class Services {
        private Services() {

        }

        public static ServiceConfig service(Object ref, Class<?> interfaceClass) {
            ServiceConfig builder = new ServiceConfig();
            builder.ref = ref;
            builder.interfaceClass = interfaceClass;
            builder.serviceName = interfaceClass.getName();

            return builder;
        }
    }

    //---------------------------------------------------------------------------------------------------------
    @Override
    public void check() {
        Preconditions.checkNotNull(this.registryConfig, "provider must need to configure register");
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
        this.registryConfig.check();
    }

    /**
     * 暴露服务
     */
    public void export() {
        if(!isExport){
            check();

            url = createServiceURL(applicationConfig, serverConfig, registryConfig, serviceName);
            Preconditions.checkNotNull(url);

            Clusters.export(url, interfaceClass, ref);

            isExport = true;
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
        if(isExport){
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ServiceConfig bind(int port){
        if(isExport){
            this.serverConfig = new ServerConfig(port);
        }
        return this;
    }

    public ServiceConfig urls(String... urls){
        if(isExport){
            this.registryConfig = new DefaultRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ServiceConfig zookeeper(String address){
        return zookeeper(address, "");
    }

    public ServiceConfig zookeeper(String address, String password){
        if(isExport){
            this.registryConfig = new DefaultRegistryConfig(address);
            this.registryConfig.setPassword(password);
        }
        return this;
    }

    public ServiceConfig sessionTimeout(int sessionTimeout){
        if(isExport){
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ServiceConfig serviceName(String serviceName){
        if(isExport){
            this.serviceName = serviceName;
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

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Object getRef() {
        return ref;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }
}
