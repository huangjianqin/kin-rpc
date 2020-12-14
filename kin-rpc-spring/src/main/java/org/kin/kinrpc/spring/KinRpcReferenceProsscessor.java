package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.spring.AbstractAnnotationBeanPostProcessor;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.AbstractRegistryConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.rpc.Notifier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
@Component
public class KinRpcReferenceProsscessor extends AbstractAnnotationBeanPostProcessor
        implements PriorityOrdered, ApplicationContextAware, BeanFactoryAware, LoggerOprs {
    @Value("${spring.application.name:kinrpc}")
    private String springAppName;
    private ApplicationContext applicationContext;
    private ConfigurableListableBeanFactory beanFactory;

    private final ConcurrentMap<String, ReferenceConfig<?>> referenceConfigs = new ConcurrentHashMap<>(32);

    public KinRpcReferenceProsscessor() {
        super(KinRpcReference.class);
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(@Nonnull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * @return reference config bean name
     */
    private String getReferenceConfigBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        String beanName = attributes.getString("beanName");
        if (!StringUtils.isNotBlank(beanName)) {
            beanName = "Reference".concat("$").concat(getAppName(attributes))
                    .concat("$").concat(getServiceName(attributes, interfaceClass));
        }
        return beanName;
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Exception {
        String referenceConfigBeanName = getReferenceConfigBeanName(attributes, injectedType);

        ReferenceConfig<?> referenceConfig = buildReferenceConfig(referenceConfigBeanName, attributes, injectedType, injectedElement);

        if (!beanFactory.containsBean(referenceConfigBeanName)) {
            //注册bean
            beanFactory.registerSingleton(referenceConfigBeanName, referenceConfig);
        }
        //缓存reference config
        referenceConfigs.put(referenceConfigBeanName, referenceConfig);

        return referenceConfig.get();
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        //相当于reference config bean name
        return getReferenceConfigBeanName(attributes, injectedType);
    }

    @Override
    public int getOrder() {
        //最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * @return 注解的appName
     */
    private String getAppName(AnnotationAttributes attributes) {
        String appName = attributes.getString("appName");
        if (StringUtils.isBlank(appName)) {
            //如果没有配置appName, 则尝试用spring application name, 最后用默认
            appName = springAppName;
        }

        return appName;
    }

    /**
     * @return 注解的服务名
     */
    private String getServiceName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        String serviceName = attributes.getString("serviceName");
        if (StringUtils.isBlank(serviceName)) {
            //如果没有配置serviceName, 则采用接口java package path命名
            serviceName = interfaceClass.getCanonicalName();
        }

        return serviceName;
    }

    /**
     * 解析注解配置, 返回{@link ReferenceConfig}
     */
    @SuppressWarnings("unchecked")
    private ReferenceConfig<?> buildReferenceConfig(String referenceConfigBeanName, AnnotationAttributes attributes, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        if (referenceConfigs.containsKey(referenceConfigBeanName)) {
            return referenceConfigs.get(referenceConfigBeanName);
        }

        String appName = getAppName(attributes);

        String serviceName = getServiceName(attributes, injectedType);

        ReferenceConfig<?> referenceConfig = References.reference(injectedType)
                .appName(appName)
                .serviceName(serviceName)
                .version(attributes.getString("version"))
                .retry(attributes.getNumber("retryTimes"))
                .retryTimeout(attributes.getNumber("retryInterval"))
                .rate(attributes.getNumber("rate"))
                .notify((Class<? extends Notifier<?>>[]) attributes.getClassArray("notifiers"))
                .callTimeout(attributes.getNumber("callTimeout"));

        //jvm
        boolean jvm = attributes.getBoolean("jvm");
        if (jvm) {
            referenceConfig.jvm();
        } else {
            //url
            String[] urls = attributes.getStringArray("urls");
            if (CollectionUtils.isNonEmpty(urls)) {
                //直连
                referenceConfig.urls(urls);
            } else {
                //寻找注册中心
                AbstractRegistryConfig registryConfig = KinRpcAnnoUtils.parseRegistryConfig((Field) injectedElement.getMember(), applicationContext);
                if (Objects.nonNull(registryConfig)) {
                    referenceConfig.registry(registryConfig);
                } else {
                    warn("reference '%s' does not config a registry, check it is all right", serviceName);
                }
            }
        }

        //loadBalance
        //1. 默认先找通过字符串定义的loadBalance
        String loadBalance = attributes.getString("loadBalance");
        if (StringUtils.isNotBlank(loadBalance)) {
            referenceConfig.loadbalance(loadBalance);
        } else {
            //2. loadBalance class
            referenceConfig.loadbalance(attributes.getClass("loadBalanceClass"));
        }

        //router
        //1. 默认先找通过字符串定义的router
        String router = attributes.getString("router");
        if (StringUtils.isNotBlank(router)) {
            referenceConfig.router(router);
        } else {
            //2. router class
            referenceConfig.router(attributes.getClass("routerClass"));
        }

        //byteCodeEnhance
        if (attributes.getBoolean("byteCodeEnhance")) {
            referenceConfig.javassistProxy();
        } else {
            referenceConfig.javaProxy();
        }

        //async
        if (attributes.getBoolean("async")) {
            referenceConfig.async();
        }

        //useGeneric
        if (attributes.getBoolean("useGeneric")) {
            referenceConfig.useGeneric();
        }

        //ssl
        if (attributes.getBoolean("ssl")) {
            referenceConfig.enableSsl();
        }

        //attachment
        Map<String, Object> attachment = KinRpcAnnoUtils.parseAttachment(attributes.getAnnotationArray("attachment", Attachment.class));
        if (CollectionUtils.isNonEmpty(attachment)) {
            referenceConfig.attach(attachment);
        }

        return referenceConfig;
    }
}
