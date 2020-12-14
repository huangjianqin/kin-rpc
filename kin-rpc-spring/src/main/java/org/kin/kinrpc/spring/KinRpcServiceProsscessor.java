package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.AbstractRegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
@Component
public class KinRpcServiceProsscessor implements BeanPostProcessor, ApplicationListener<ContextClosedEvent>,
        ApplicationContextAware, ApplicationEventPublisherAware, LoggerOprs {
    @Value("${spring.application.name:kinrpc}")
    private String springAppName;

    private ApplicationContext applicationContext;
    private ApplicationEventPublisher applicationEventPublisher;
    /** key -> bean name, value -> ServiceConfig */
    private Map<String, ServiceConfig<?>> beanName2ServiceConfig = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).registerShutdownHook();
        }
    }

    @Override
    public void setApplicationEventPublisher(@Nonnull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onApplicationEvent(@Nonnull ContextClosedEvent contextClosedEvent) {
        //spring容器关闭时, un exported
        for (ServiceConfig<?> config : beanName2ServiceConfig.values()) {
            config.disable();
        }
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        if (beanClass.isAnnotationPresent(KinRpcService.class)) {
            //有注解才需要export services
            try {
                export(beanClass, bean, beanName);
            } catch (Exception e) {
                error("service export fail due to ", e);
            }
        }

        return bean;
    }

    /**
     * export service bean
     */
    @SuppressWarnings("unchecked")
    private void export(Class<?> beanClass, Object bean, String beanName) {
        KinRpcService serviceAnno = beanClass.getAnnotation(KinRpcService.class);
        Class interfaceClass = serviceAnno.interfaceClass();
        if (!interfaceClass.isAssignableFrom(beanClass)) {
            //接口没对上
            return;
        }

        String appName = serviceAnno.appName();
        if (StringUtils.isBlank(appName)) {
            //如果没有配置appName, 则尝试用spring application name, 最后用默认
            appName = springAppName;
        }

        String serviceName = serviceAnno.serviceName();
        if (StringUtils.isBlank(serviceName)) {
            //如果没有配置serviceName, 则采用接口java package path命名
            serviceName = interfaceClass.getCanonicalName();
        }

        //如果自定义了serializer, 优先使用自定义的serializer
        int serializerCode = serviceAnno.serializerCode();
        if (serializerCode <= 0) {
            serializerCode = serviceAnno.serializerType().getCode();
        }

        //注入配置
        ServiceConfig serviceConfig = Services.service(bean, interfaceClass)
                .appName(appName)
                .bind(serviceAnno.host(), serviceAnno.port())
                .serviceName(serviceName)
                .version(serviceAnno.version())
                .serializer(serializerCode)
                .compress(serviceAnno.compressionType())
                .rate(serviceAnno.rate())
                .protocol(serviceAnno.protocolType());

        //寻找注册中心
        AbstractRegistryConfig registryConfig = KinRpcAnnoUtils.parseRegistryConfig(beanClass, applicationContext);
        if (Objects.nonNull(registryConfig)) {
            serviceConfig.registry(registryConfig);
        } else {
            warn("service '{}' has not registry", serviceName);
        }

        //byteCodeEnhance
        if (serviceAnno.byteCodeEnhance()) {
            serviceConfig.javassistProxy();
        } else {
            serviceConfig.javaProxy();
        }

        //actorLike
        if (serviceAnno.actorLike()) {
            serviceConfig.actorLike();
        }

        //ssl
        if (serviceAnno.ssl()) {
            serviceConfig.enableSsl();
        }

        //attachment
        Map<String, Object> attachment = KinRpcAnnoUtils.parseAttachment(serviceAnno.attachment());
        if (CollectionUtils.isNonEmpty(attachment)) {
            serviceConfig.attach(attachment);
        }

        serviceConfig.export();
        beanName2ServiceConfig.put(beanName, serviceConfig);

        //推事件
        applicationEventPublisher.publishEvent(new ServiceExportedEvent(bean));
    }
}
