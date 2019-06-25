package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.cluster.Clusters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ReferenceConfig<T> extends AbstractConfig {
    private ApplicationConfig applicationConfig;
    private AbstractRegistryConfig registryConfig;
    private Class<T> interfaceClass;
    private int timeout = Constants.REFERENCE_DEFAULT_CONNECT_TIMEOUT;
    private String serviceName;
    private int retryTimes = Constants.RETRY_TIMES;
    private int retryTimeout = Constants.RETRY_TIMEOUT;

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
        Preconditions.checkArgument(this.timeout <= 0, "connection's timeout must greater than 0");
        Preconditions.checkArgument(this.retryTimeout <= 0, "retrytimeout must greater than 0");
    }

    public synchronized T get() {
        if(!isReference){
            check();

            Map<String, String> params = new HashMap<>();
            params.put(Constants.TIMEOUT_KEY, timeout + "");
            params.put(Constants.RETRY_TIMES_KEY, retryTimes + "");
            params.put(Constants.RETRY_TIMEOUT_KEY, retryTimeout + "");

            url = createReferenceURL(applicationConfig, registryConfig, serviceName, params);
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

    public ReferenceConfig<T> retry(int retryTimes){
        if(isReference){
            this.retryTimes = retryTimes;
        }
        return this;
    }

    public ReferenceConfig<T> retryTimeout(int retryTimeout){
        if(isReference){
            this.retryTimeout = retryTimeout;
        }
        return this;
    }

    //setter && getter
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public AbstractRegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public int getTimeout() {
        return timeout;
    }
}
