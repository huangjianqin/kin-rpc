package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.AbstractRegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.rpc.common.Constants;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理{@link KinRpcService}注解在bean class的场景
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
@SuppressWarnings("rawtypes")
final class KinRpcServiceBeanProcessor implements BeanPostProcessor, ApplicationListener<ContextClosedEvent>,
        ApplicationContextAware, ApplicationEventPublisherAware, LoggerOprs, DisposableBean {
    /** spring application name */
    @Value("${spring.application.name:kinrpc}")
    private String springAppName;
    /** spring boot server port */
    @Value("${server.port:0}")
    private int springServerPort;

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
        destroy();
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        //解决spring 自带的java proxy | cglib代理问题, bean.getclass在javassist会找不到对应的class
        KinRpcService serviceAnno = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(bean), KinRpcService.class);
        if (Objects.nonNull(serviceAnno)) {
            //有注解才需要export services
            try {
                exportService(serviceAnno, bean, beanName);
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
    private void exportService(KinRpcService serviceAnno, Object bean, String beanName) {
        Class interfaceClass = serviceAnno.interfaceClass();
        Class<?> beanClass = bean.getClass();
        if (!interfaceClass.isAssignableFrom(beanClass)) {
            //接口没对上
            return;
        }

        String appName = serviceAnno.appName();
        if (StringUtils.isBlank(appName)) {
            //如果没有配置appName, 则尝试用spring application name
            appName = springAppName;
            if (StringUtils.isBlank(appName)) {
                //用默认
                appName = "kinrpc";
            }
        }

        String serviceName = serviceAnno.serviceName();
        if (StringUtils.isBlank(serviceName)) {
            //如果没有配置serviceName, 则采用接口java package path命名
            serviceName = interfaceClass.getCanonicalName();
        }

        //如果自定义了serialization, 优先使用自定义的serialization
        int serializationCode = serviceAnno.serializationCode();
        if (serializationCode <= 0) {
            serializationCode = serviceAnno.serializationType().getCode();
        }

        //解析服务绑定端口
        //1. developer custom
        int port = serviceAnno.port();
        if (port <= 0) {
            if (springServerPort > 0) {
                //2. sprint boot server port
                port = springServerPort;
            } else {
                //3. default port
                port = Constants.SERVER_DEFAULT_PORT;
            }
        }

        //注入配置
        ServiceConfig serviceConfig = Services.service(bean, interfaceClass)
                .appName(appName)
                .bind(serviceAnno.host(), port)
                .serviceName(serviceName)
                .version(serviceAnno.version())
                .serialization(serializationCode)
                .compress(serviceAnno.compressionType())
                .tps(serviceAnno.tps())
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
        applicationEventPublisher.publishEvent(new ServiceExportedEvent(serviceConfig, bean));
    }

    @Override
    public void destroy() {
        //spring容器关闭时, un exported
        for (ServiceConfig<?> config : beanName2ServiceConfig.values()) {
            config.disable();
        }
        beanName2ServiceConfig.clear();
    }
}
