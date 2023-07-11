package org.kin.kinrpc.boot;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/10
 */
public class KinRpcBootstrapApplicationListener implements ApplicationListener<ApplicationEvent> {
    @Value("${spring.application.name:kinRpcApp}")
    private String springAppName;
    @Autowired
    private KinRpcProperties properties;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshed(((ContextRefreshedEvent) event).getApplicationContext());
        } else if (event instanceof ContextStoppedEvent) {
            onContextStopped();
        }
    }

    /**
     * spring context refreshed后执行
     */
    private void onContextRefreshed(ApplicationContext applicationContext) {
        KinRpcBootstrap bootstrap = KinRpcBootstrap.instance();
        initGlobalConfig(applicationContext, bootstrap);
        bootstrap.start();
        JvmCloseCleaner.instance().add(bootstrap::destroy);
    }

    /**
     * 初始化全局配置
     *
     * @param bootstrap kinrpc bootstrap
     */
    private void initGlobalConfig(ApplicationContext applicationContext,
                                  KinRpcBootstrap bootstrap) {
        String appName = StringUtils.isNotBlank(properties.getAppName()) ? properties.getAppName() : springAppName;
        bootstrap.app(appName);

        //combine
        List<ExecutorConfig> executors = new ArrayList<>(4);
        List<RegistryConfig> registries = new ArrayList<>(4);
        List<ServerConfig> servers = new ArrayList<>(4);
        List<KinRpcProperties.ProviderProperty> providers = new ArrayList<>(4);
        List<KinRpcProperties.ConsumerProperty> consumers = new ArrayList<>(4);

        if (Objects.nonNull(properties.getExecutor())) {
            executors.add(properties.getExecutor());
        }

        if (Objects.nonNull(properties.getRegistry())) {
            registries.add(properties.getRegistry());
        }

        if (Objects.nonNull(properties.getServer())) {
            servers.add(properties.getServer());
        }

        if (Objects.nonNull(properties.getProvider())) {
            providers.add(properties.getProvider());
        }

        if (Objects.nonNull(properties.getConsumer())) {
            consumers.add(properties.getConsumer());
        }

        if (CollectionUtils.isNonEmpty(properties.getExecutors())) {
            executors.addAll(properties.getExecutors());
        }

        if (CollectionUtils.isNonEmpty(properties.getRegistries())) {
            registries.addAll(properties.getRegistries());
        }

        if (CollectionUtils.isNonEmpty(properties.getServers())) {
            servers.addAll(properties.getServers());
        }

        if (CollectionUtils.isNonEmpty(properties.getProviders())) {
            providers.addAll(properties.getProviders());
        }

        if (CollectionUtils.isNonEmpty(properties.getConsumers())) {
            consumers.addAll(properties.getConsumers());
        }

        //set
        if (CollectionUtils.isNonEmpty(executors)) {
            bootstrap.executors(executors);
        }

        if (CollectionUtils.isNonEmpty(registries)) {
            bootstrap.registries(registries);
        }

        if (CollectionUtils.isNonEmpty(servers)) {
            bootstrap.servers(servers);
        }

        if (CollectionUtils.isNonEmpty(providers)) {
            for (KinRpcProperties.ProviderProperty providerProperty : providers) {
                FilterUtils.addFilters(applicationContext, providerProperty::getFilterBeans, providerProperty::filter);
            }
            bootstrap.providers(new ArrayList<>(providers));
        }

        if (CollectionUtils.isNonEmpty(consumers)) {
            for (KinRpcProperties.ConsumerProperty consumerProperty : consumers) {
                FilterUtils.addFilters(applicationContext, consumerProperty::getFilterBeans, consumerProperty::filter);
            }
            bootstrap.consumers(new ArrayList<>(consumers));
        }
    }

    /**
     * spring context stopped后执行
     */
    private void onContextStopped() {
        KinRpcBootstrap.instance().destroy();
    }
}
