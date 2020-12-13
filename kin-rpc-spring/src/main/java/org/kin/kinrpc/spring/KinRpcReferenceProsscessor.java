package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.AbstractRegistryConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
@Component
public class KinRpcReferenceProsscessor implements ApplicationContextAware, LoggerOprs {
    @Value("${spring.application.name:kinrpc}")
    private String springAppName;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取需要注入的实例, 即kinRpcReference实例
     */
    private Object kinRpcReference(Field field) {
        KinRpcReference referenceAnno = field.getAnnotation(KinRpcReference.class);
        if (Objects.isNull(referenceAnno)) {
            throw new IllegalStateException("field does not have @KinRpcReference");
        }

        Class<?> interfaceClass = field.getType();
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("field with @KinRpcReference must be a interface");
        }

        String appName = referenceAnno.appName();
        if (StringUtils.isBlank(appName)) {
            //如果没有配置appName, 则尝试用spring application name, 最后用默认
            appName = springAppName;
        }

        String serviceName = referenceAnno.serviceName();
        if (StringUtils.isBlank(serviceName)) {
            //如果没有配置serviceName, 则采用接口java package path命名
            serviceName = interfaceClass.getCanonicalName();
        }

        ReferenceConfig<?> referenceConfig = References.reference(interfaceClass)
                .appName(appName)
                .serviceName(serviceName)
                .version(referenceAnno.version())
                .retry(referenceAnno.retryTimes())
                .retryTimeout(referenceAnno.retryInterval())
                .rate(referenceAnno.rate())
                .notify(referenceAnno.notifiers())
                .callTimeout(referenceAnno.callTimeout());

        //jvm
        boolean jvm = referenceAnno.jvm();
        if (jvm) {
            referenceConfig.jvm();
        } else {
            //url
            String[] urls = referenceAnno.urls();
            if (CollectionUtils.isNonEmpty(urls)) {
                //直连
                referenceConfig.urls(urls);
            } else {
                //寻找注册中心
                AbstractRegistryConfig registryConfig = KinRpcAnnoUtils.parseRegistryConfig(field, applicationContext);
                if (Objects.nonNull(registryConfig)) {
                    referenceConfig.registry(registryConfig);
                } else {
                    warn("reference '%s' does not config a registry, check it is all right", serviceName);
                }
            }
        }

        //loadBalance
        //1. 默认先找通过字符串定义的loadBalance
        String loadBalance = referenceAnno.loadBalance();
        if (StringUtils.isNotBlank(loadBalance)) {
            referenceConfig.loadbalance(loadBalance);
        } else {
            //2. loadBalance class
            referenceConfig.loadbalance(referenceAnno.loadBalanceClass());
        }

        //router
        //1. 默认先找通过字符串定义的router
        String router = referenceAnno.router();
        if (StringUtils.isNotBlank(router)) {
            referenceConfig.router(router);
        } else {
            //2. router class
            referenceConfig.router(referenceAnno.routerClass());
        }

        //byteCodeEnhance
        if (referenceAnno.byteCodeEnhance()) {
            referenceConfig.javassistProxy();
        } else {
            referenceConfig.javaProxy();
        }

        //async
        if (referenceAnno.async()) {
            referenceConfig.async();
        }

        //useGeneric
        if (referenceAnno.useGeneric()) {
            referenceConfig.useGeneric();
        }

        //ssl
        if (referenceAnno.ssl()) {
            referenceConfig.enableSsl();
        }

        //attachment
        Map<String, Object> attachment = KinRpcAnnoUtils.parseAttachment(referenceAnno.attachment());
        if (CollectionUtils.isNonEmpty(attachment)) {
            referenceConfig.attach(attachment);
        }

        return referenceConfig.get();
    }
}
