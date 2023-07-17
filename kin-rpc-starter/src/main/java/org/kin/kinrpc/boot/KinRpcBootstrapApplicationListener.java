package org.kin.kinrpc.boot;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.bootstrap.KinRpcBootstrapListener;
import org.kin.kinrpc.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

        if (properties.isAsyncExportRefer()) {
            bootstrap.asyncExportRefer();
        }

        //listener bean
        Map<String, KinRpcBootstrapListener> kinRpcBootstrapListenerMap = applicationContext.getBeansOfType(KinRpcBootstrapListener.class);
        Collection<KinRpcBootstrapListener> kinRpcBootstrapListeners = kinRpcBootstrapListenerMap.values();
        if (CollectionUtils.isNonEmpty(kinRpcBootstrapListeners)) {
            bootstrap.listeners(kinRpcBootstrapListeners);
        }

        configBootstrap(applicationContext, ExecutorConfig.class, bootstrap::executors, properties::getExecutor, properties::getExecutors);
        configBootstrap(applicationContext, RegistryConfig.class, bootstrap::registries, properties::getRegistry, properties::getRegistries);
        configBootstrap(applicationContext, ServerConfig.class, bootstrap::servers, properties::getServer, properties::getServers);
        configBootstrap(applicationContext, ProviderConfig.class, pcs -> {
                    for (ProviderConfig pc : pcs) {
                        if (!(pc instanceof KinRpcProperties.ProviderProperty)) {
                            continue;
                        }
                        KinRpcProperties.ProviderProperty pp = (KinRpcProperties.ProviderProperty) pc;
                        FilterUtils.addFilters(applicationContext, pp::getFilterBeans, pp::filter);
                    }
                },
                bootstrap::providers,
                () -> properties.getProvider(),
                () -> CollectionUtils.isNonEmpty(properties.getProviders()) ? new ArrayList<>(properties.getProviders()) : Collections.emptyList());
        configBootstrap(applicationContext, ConsumerConfig.class, ccs -> {
                    for (ConsumerConfig cc : ccs) {
                        if (!(cc instanceof KinRpcProperties.ConsumerProperty)) {
                            continue;
                        }
                        KinRpcProperties.ConsumerProperty cp = (KinRpcProperties.ConsumerProperty) cc;
                        FilterUtils.addFilters(applicationContext, cp::getFilterBeans, cp::filter);
                    }
                },
                bootstrap::consumers,
                () -> properties.getConsumer(),
                () -> CollectionUtils.isNonEmpty(properties.getConsumers()) ? new ArrayList<>(properties.getConsumers()) : Collections.emptyList());
        configBootstrap(applicationContext, ServiceConfig.class, bootstrap::services);
        configBootstrap(applicationContext, ReferenceConfig.class, bootstrap::references);
    }

    /**
     * 将以{@link org.springframework.context.annotation.Bean}形式注册的{@link Config}实例添加到{@code configs}
     *
     * @param applicationContext spring application context
     * @param configClass        config class
     * @param configs            temp config cache
     */
    private <C extends Config> void getConfigFromBeans(ApplicationContext applicationContext,
                                                       Class<C> configClass,
                                                       List<C> configs) {
        Map<String, C> configBeanMap = applicationContext.getBeansOfType(configClass);
        if (CollectionUtils.isNonEmpty(configBeanMap)) {
            configs.addAll(configBeanMap.values());
        }
    }

    /**
     * {@link KinRpcBootstrap}设置配置
     *
     * @param applicationContext spring application context
     * @param configClass        config class
     * @param setter             bootstrap setup config func
     * @param getter             get single config from {@link KinRpcProperties} bean
     * @param batchGetter        batch get configs from {@link KinRpcProperties} bean
     */
    private <C extends Config> void configBootstrap(ApplicationContext applicationContext,
                                                    Class<C> configClass,
                                                    Consumer<List<C>> setter,
                                                    Supplier<C> getter,
                                                    Supplier<Collection<C>> batchGetter) {
        configBootstrap(applicationContext, configClass, null, setter, getter, batchGetter);
    }

    /**
     * {@link KinRpcBootstrap}设置配置
     *
     * @param applicationContext spring application context
     * @param configClass        config class
     * @param setter             bootstrap setup config func
     */
    private <C extends Config> void configBootstrap(ApplicationContext applicationContext,
                                                    Class<C> configClass,
                                                    Consumer<List<C>> setter) {
        configBootstrap(applicationContext, configClass, null, setter, null, null);
    }

    /**
     * {@link KinRpcBootstrap}设置配置
     *
     * @param applicationContext spring application context
     * @param configClass        config class
     * @param funcBeforeSet      operation before bootstrap setup config
     * @param setter             bootstrap setup config func
     * @param getter             get single config from {@link KinRpcProperties} bean
     * @param batchGetter        batch get configs from {@link KinRpcProperties} bean
     */
    private <C extends Config> void configBootstrap(ApplicationContext applicationContext,
                                                    Class<C> configClass,
                                                    Consumer<List<C>> funcBeforeSet,
                                                    Consumer<List<C>> setter,
                                                    Supplier<C> getter,
                                                    Supplier<Collection<C>> batchGetter) {
        List<C> configs = new ArrayList<>(8);

        //properties
        if (Objects.nonNull(getter) &&
                Objects.nonNull(getter.get())) {
            configs.add(getter.get());
        }

        if (Objects.nonNull(batchGetter) &&
                CollectionUtils.isNonEmpty(batchGetter.get())) {
            configs.addAll(batchGetter.get());
        }

        //beans
        getConfigFromBeans(applicationContext, configClass, configs);

        //extra operation before set
        if (Objects.nonNull(funcBeforeSet)) {
            funcBeforeSet.accept(configs);
        }

        //set
        if (CollectionUtils.isNonEmpty(configs)) {
            setter.accept(configs);
        }
    }

    /**
     * spring context stopped后执行
     */
    private void onContextStopped() {
        KinRpcBootstrap.instance().destroy();
    }
}
