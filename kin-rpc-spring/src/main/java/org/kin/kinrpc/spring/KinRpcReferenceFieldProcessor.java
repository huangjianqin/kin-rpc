package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.spring.beans.AbstractAnnotationBeanPostProcessor;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ReferenceConfig;
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

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 处理{@link KinRpcReference}注解在bean class成员域field的场景
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
final class KinRpcReferenceFieldProcessor extends AbstractAnnotationBeanPostProcessor
        implements PriorityOrdered, ApplicationContextAware, BeanFactoryAware, LoggerOprs {
    @Value("${spring.application.name:kinrpc}")
    private String springAppName;
    private ApplicationContext applicationContext;
    private ConfigurableListableBeanFactory beanFactory;

    private final ConcurrentMap<String, ReferenceConfig<?>> referenceConfigs = new ConcurrentHashMap<>(32);

    KinRpcReferenceFieldProcessor() {
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
            StringJoiner sj = new StringJoiner("$");
            sj.add("Reference");
            sj.add(KinRpcAnnoUtils.getAppName(attributes, springAppName));
            sj.add(KinRpcAnnoUtils.getServiceName(attributes, interfaceClass));
            sj.add(attributes.getString("version"));
        }
        return beanName;
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Exception {
        if (!injectedType.isInterface()) {
            throw new IllegalArgumentException("annotation '@KinRpcReference' must be used with interface");
        }

        String referenceConfigBeanName = getReferenceConfigBeanName(attributes, injectedType);

        ReferenceConfig<?> referenceConfig = getOrCreateReferenceConfig(referenceConfigBeanName, attributes, injectedType, injectedElement);
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
     * 解析注解配置, 返回{@link ReferenceConfig}
     */
    @SuppressWarnings("unchecked")
    private ReferenceConfig<?> getOrCreateReferenceConfig(String referenceConfigBeanName, AnnotationAttributes attributes, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        ReferenceConfig<?> referenceConfig = referenceConfigs.get(referenceConfigBeanName);
        if (Objects.isNull(referenceConfig)) {
            referenceConfig = KinRpcAnnoUtils.convert2ReferenceCfg(applicationContext, attributes, injectedType, injectedElement, springAppName);
        }
        return referenceConfig;
    }

    @Override
    public void destroy() throws Exception {
        for (ReferenceConfig<?> referenceConfig : referenceConfigs.values()) {
            referenceConfig.disable();
        }
        referenceConfigs.clear();

        super.destroy();
    }
}
