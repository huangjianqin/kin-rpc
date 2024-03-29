package org.kin.kinrpc.boot;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * 处理{@link KinRpcService}注解
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public class KinRpcServiceBeanProcessor implements BeanPostProcessor, ApplicationContextAware, BeanFactoryAware {
    /** spring application context */
    private ApplicationContext applicationContext;
    /** spring bean factory */
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        //获取BeanDefinition
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (beanDefinition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;

            //尝试从class获取@KinRpcService注解属性
            AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
            Map<String, Object> annoAttrs = metadata.getAnnotationAttributes(KinRpcService.class.getName());
            if (CollectionUtils.isNonEmpty(annoAttrs)) {
                addServiceConfig(annoAttrs, bean);
            }

            //尝试从工厂方法(@Bean)获取@KinRpcService注解属性
            MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
            if (Objects.nonNull(factoryMethodMetadata)) {
                annoAttrs = factoryMethodMetadata.getAnnotationAttributes(KinRpcService.class.getName());
                if (CollectionUtils.isNonEmpty(annoAttrs)) {
                    addServiceConfig(annoAttrs, bean);
                }
            }
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 注册service config
     *
     * @param serviceAnnoAttrs {@link KinRpcService}属性
     */
    @SuppressWarnings("unchecked")
    private void addServiceConfig(Map<String, Object> serviceAnnoAttrs, Object bean) {
        Class<? super Object> interfaceClass = (Class<? super Object>) serviceAnnoAttrs.get("interfaceClass");
        ServiceConfig<?> serviceConfig = ServiceConfig.create(interfaceClass, bean);

        String[] registries = (String[]) serviceAnnoAttrs.get("registries");
        if (CollectionUtils.isNonEmpty(registries)) {
            for (String registryConfigId : registries) {
                serviceConfig.registries(RegistryConfig.fromId(registryConfigId));
            }
        }

        String group = (String) serviceAnnoAttrs.get("group");
        if (StringUtils.isNotBlank(group)) {
            serviceConfig.group(group);
        }

        String serviceName = (String) serviceAnnoAttrs.get("serviceName");
        if (StringUtils.isBlank(serviceName)) {
            serviceName = interfaceClass.getName();
        }
        serviceConfig.serviceName(serviceName);

        String version = (String) serviceAnnoAttrs.get("version");
        if (StringUtils.isNotBlank(version)) {
            serviceConfig.version(version);
        }

        String serialization = (String) serviceAnnoAttrs.get("serialization");
        if (StringUtils.isNotBlank(serialization)) {
            serviceConfig.serialization(serialization);
        }

        String[] filters = (String[]) serviceAnnoAttrs.get("filters");
        if (CollectionUtils.isNonEmpty(filters)) {
            FilterUtils.addFilters(applicationContext, () -> Arrays.asList(filters), serviceConfig::filter);
        }

        if ((Boolean) serviceAnnoAttrs.get("jvm")) {
            serviceConfig.jvm();
        }

        String[] servers = (String[]) serviceAnnoAttrs.get("servers");
        if (CollectionUtils.isNonEmpty(servers)) {
            for (String serverConfigId : servers) {
                serviceConfig.servers(ServerConfig.fromId(serverConfigId));
            }
        }

        String executor = (String) serviceAnnoAttrs.get("executor");
        if (StringUtils.isNotBlank(executor)) {
            serviceConfig.executor(ExecutorConfig.fromId(executor));
        }

        int weight = (int) serviceAnnoAttrs.get("weight");
        if (weight > 0) {
            serviceConfig.weight(weight);
        }

        String token = (String) serviceAnnoAttrs.get("token");
        if (StringUtils.isNotBlank(token)) {
            serviceConfig.token(token);
        }

        long delay = (long) serviceAnnoAttrs.get("delay");
        if (delay > 0) {
            serviceConfig.delay(delay);
        }

        boolean exportAsync = (boolean) serviceAnnoAttrs.get("exportAsync");
        if (exportAsync) {
            serviceConfig.exportAsync();
        }

        KinRpcBootstrap.instance().service(serviceConfig);
    }
}
