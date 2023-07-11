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

            //从class获取@KinRpcService注解属性
            AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
            Map<String, Object> annoAttrs = metadata.getAnnotationAttributes(KinRpcService.class.getName());
            if (CollectionUtils.isNonEmpty(annoAttrs)) {
                addServiceConfig(annoAttrs, bean);
            }

            //从工厂方法(@Bean)获取@KinRpcService注解属性
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
     * @param serviceAnno {@link KinRpcService}属性
     */
    @SuppressWarnings("unchecked")
    private void addServiceConfig(Map<String, Object> serviceAnno, Object bean) {
        Class<? super Object> interfaceClass = (Class<? super Object>) serviceAnno.get("interfaceClass");
        ServiceConfig<?> serviceConfig = ServiceConfig.create(interfaceClass, bean)
                .group((String) serviceAnno.get("group"))
                .version((String) serviceAnno.get("version"))
                .serialization((String) serviceAnno.get("serialization"))
                .weight((Integer) serviceAnno.get("weight"));

        String[] registries = (String[]) serviceAnno.get("registries");
        if (CollectionUtils.isNonEmpty(registries)) {
            for (String registryConfigId : registries) {
                serviceConfig.registries(RegistryConfig.fromId(registryConfigId));
            }
        }

        String serviceName = (String) serviceAnno.get("serviceName");
        if (StringUtils.isBlank(serviceName)) {
            serviceName = interfaceClass.getName();
        }
        serviceConfig.serviceName(serviceName);

        String[] filters = (String[]) serviceAnno.get("filters");
        if (CollectionUtils.isNonEmpty(filters)) {
            FilterUtils.addFilters(applicationContext, () -> Arrays.asList(filters), serviceConfig::filter);
        }

        if ((Boolean) serviceAnno.get("jvm")) {
            serviceConfig.jvm();
        }

        String[] servers = (String[]) serviceAnno.get("servers");
        if (CollectionUtils.isNonEmpty(servers)) {
            for (String serverConfigId : servers) {
                serviceConfig.servers(ServerConfig.fromId(serverConfigId));
            }
        }

        String executor = (String) serviceAnno.get("executor");
        if (StringUtils.isNotBlank(executor)) {
            serviceConfig.executor(ExecutorConfig.fromId(executor));
        }

        String token = (String) serviceAnno.get("token");
        if (StringUtils.isNotBlank(token)) {
            serviceConfig.token(token);
        }

        long delay = (long) serviceAnno.get("delay");
        if (delay > 0) {
            serviceConfig.delay(delay);
        }

        KinRpcBootstrap.instance().service(serviceConfig);
    }
}
