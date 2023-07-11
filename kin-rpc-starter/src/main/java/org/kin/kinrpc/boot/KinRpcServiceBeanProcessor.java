package org.kin.kinrpc.boot;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

/**
 * 处理{@link KinRpcService}注解在bean class的场景
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public class KinRpcServiceBeanProcessor implements BeanPostProcessor, ApplicationContextAware {
    /** spring application context */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        //解决spring 自带的java proxy | cglib代理问题, bean.getclass在javassist会找不到对应的class
        KinRpcService serviceAnno = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(bean), KinRpcService.class);
        if (Objects.nonNull(serviceAnno)) {
            addServiceConfig(serviceAnno, bean);
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 注册service config
     *
     * @param serviceAnno kinrpc service注解
     */
    @SuppressWarnings("unchecked")
    private void addServiceConfig(KinRpcService serviceAnno, Object bean) {
        ServiceConfig<?> serviceConfig = ServiceConfig.create((Class<? super Object>) serviceAnno.interfaceClass(), bean)
                .group(serviceAnno.group())
                .version(serviceAnno.version())
                .serialization(serviceAnno.serialization())
                .weight(serviceAnno.weight());
        if (CollectionUtils.isNonEmpty(serviceAnno.registries())) {
            for (String registryConfigId : serviceAnno.registries()) {
                serviceConfig.registries(RegistryConfig.fromId(registryConfigId));
            }
        }

        String serviceName = serviceAnno.serviceName();
        if (StringUtils.isBlank(serviceName)) {
            serviceName = serviceAnno.interfaceClass().getName();
        }
        serviceConfig.serviceName(serviceName);

        if (CollectionUtils.isNonEmpty(serviceAnno.filter())) {
            FilterUtils.addFilters(applicationContext, () -> Arrays.asList(serviceAnno.filter()), serviceConfig::filter);
        }

        if (serviceAnno.jvm()) {
            serviceConfig.jvm();
        }

        if (CollectionUtils.isNonEmpty(serviceAnno.servers())) {
            for (String serverConfigId : serviceAnno.servers()) {
                serviceConfig.servers(ServerConfig.fromId(serverConfigId));
            }
        }

        if (StringUtils.isNotBlank(serviceAnno.executor())) {
            serviceConfig.executor(ExecutorConfig.fromId(serviceAnno.executor()));
        }

        if (StringUtils.isNotBlank(serviceAnno.token())) {
            serviceConfig.token(serviceAnno.token());
        }

        if ((serviceAnno.delay() > 0)) {
            serviceConfig.delay(serviceAnno.delay());
        }

        KinRpcBootstrap.instance().service(serviceConfig);
    }
}
