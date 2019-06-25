package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.cluster.Clusters;

import java.util.Collections;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ReferenceConfig<T> extends Config {
    private ApplicationConfig applicationConfig;
    private RegistryConfig registryConfig;
    private Class<T> interfaceClass;
    private int timeout = Constants.REFERENCE_DEFAULT_CONNECT_TIMEOUT;//默认5s
    private String serviceName;

    private URL url;
    private volatile T reference;
    private boolean isReference;

    private ReferenceConfig(){}

    public static class References{
        private References(){

        }

        public static <T> ReferenceConfig<T> reference(Class<T> interfaceClass) {
            ReferenceConfig builder = new ReferenceConfig();
            builder.interfaceClass = interfaceClass;
            builder.serviceName = interfaceClass.getName();

            return builder;
        }
    }

    //---------------------------------------------------------------------------------------------------------
    @Override
    public void check() {
        Preconditions.checkNotNull(this.applicationConfig, "consumer must need to configure application");
        Preconditions.checkNotNull(this.registryConfig, "consumer must need to configure register");
        Preconditions.checkNotNull(this.interfaceClass, "consumer subscribed interface must be not null");
        Preconditions.checkNotNull(this.timeout < 0, "connection's timeout must greater than 0");

    }

    public synchronized T get() {
        if(!isReference){
            check();

            url = createReferenceURL(applicationConfig, registryConfig, serviceName,
                    Collections.singletonMap(Constants.TIMEOUT, timeout + ""));
            Preconditions.checkNotNull(url);

            reference = Clusters.reference(url, interfaceClass);

            isReference = true;
        }

        return reference;
    }

    public void disable() {
        if(isReference){
            reference = null;

            Clusters.disableReference(url);
        }
    }

    //---------------------------------------builder------------------------------------------------------------
    public ReferenceConfig<T> appName(String appName) {
        if(isReference){
            this.applicationConfig = new ApplicationConfig(appName);
        }
        return this;
    }

    public ReferenceConfig<T> urls(String... urls){
        if(isReference){
            this.registryConfig = new DefaultRegistryConfig(StringUtils.mkString(";", urls));
        }
        return this;
    }

    public ReferenceConfig<T> zookeeper(String address){
        return zookeeper(address, "");
    }

    public ReferenceConfig<T> zookeeper(String address, String password){
        if(isReference){
            this.registryConfig = new DefaultRegistryConfig(address);
            this.registryConfig.setPassword(password);
        }
        return this;
    }

    public ReferenceConfig<T> sessionTimeout(int sessionTimeout){
        if(isReference){
            this.registryConfig.setSessionTimeout(sessionTimeout);
        }
        return this;
    }

    public ReferenceConfig<T> timeout(int timeout){
        if(isReference){
            this.timeout = timeout;
        }
        return this;
    }

    public ReferenceConfig<T> serviceName(String serviceName){
        if(isReference){
            this.serviceName = serviceName;
        }
        return this;
    }

    //setter && getter
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public int getTimeout() {
        return timeout;
    }
}
