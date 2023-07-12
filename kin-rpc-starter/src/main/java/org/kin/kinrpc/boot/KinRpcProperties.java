package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
@ConfigurationProperties("kinrpc")
public class KinRpcProperties {
    /** 应用名 */
    private String appName;
    /** 标识是否异步export或refer */
    private boolean asyncExportRefer;

    /** 全局executor config */
    @NestedConfigurationProperty
    private ExecutorConfig executor;
    /** 全局registry config */
    @NestedConfigurationProperty
    private RegistryConfig registry;
    /** 全局server config */
    @NestedConfigurationProperty
    private ServerConfig server;
    /** !!!!无用, 纯粹为了生成spring yml doc */
    @NestedConfigurationProperty
    private SslConfig ssl;
    /** provider config */
    private ProviderProperty provider;
    /** consumer config */
    private ConsumerProperty consumer;

    /** 全局executor config list */
    private List<ExecutorConfig> executors;
    /** 全局registry config list */
    private List<RegistryConfig> registries;
    /** 全局server config list */
    private List<ServerConfig> servers;
    /** provider config list */
    private List<ProviderProperty> providers;
    /** consumer config list */
    private List<ConsumerProperty> consumers;

    //setter && getter
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public ExecutorConfig getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorConfig executor) {
        this.executor = executor;
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public void setSsl(SslConfig ssl) {
        this.ssl = ssl;
    }

    public ProviderProperty getProvider() {
        return provider;
    }

    public void setProvider(ProviderProperty provider) {
        this.provider = provider;
    }

    public ConsumerProperty getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerProperty consumer) {
        this.consumer = consumer;
    }

    public List<ExecutorConfig> getExecutors() {
        return executors;
    }

    public void setExecutors(List<ExecutorConfig> executors) {
        this.executors = executors;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers;
    }

    public List<ProviderProperty> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderProperty> providers) {
        this.providers = providers;
    }

    public List<ConsumerProperty> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<ConsumerProperty> consumers) {
        this.consumers = consumers;
    }

    public boolean isAsyncExportRefer() {
        return asyncExportRefer;
    }

    public void setAsyncExportRefer(boolean asyncExportRefer) {
        this.asyncExportRefer = asyncExportRefer;
    }

    //----------------------------------------------------------------------------------------------------------------------------------------------------------

    /** {@link ProviderConfig}扩展 */
    static class ProviderProperty extends ProviderConfig {
        /** filter bean name list */
        private List<String> filterBeans;

        //setter && getter
        public List<String> getFilterBeans() {
            return filterBeans;
        }

        public void setFilterBeans(List<String> filterBeans) {
            this.filterBeans = filterBeans;
        }
    }

    /** {@link ConsumerConfig}扩展 */
    static class ConsumerProperty extends ConsumerConfig {
        /** filter bean name list */
        private List<String> filterBeans;

        //setter && getter
        public List<String> getFilterBeans() {
            return filterBeans;
        }

        public void setFilterBeans(List<String> filterBeans) {
            this.filterBeans = filterBeans;
        }
    }
}
