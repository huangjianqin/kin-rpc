package org.kin.kinrpc.boot;

import org.kin.framework.spring.beans.AbstractAnnotationBeanPostProcessor;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.ReferenceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;

import javax.annotation.Nonnull;
import java.util.StringJoiner;

/**
 * 处理{@link KinRpcReference}注解在bean class成员域field的场景
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public class KinRpcReferenceFieldProcessor extends AbstractAnnotationBeanPostProcessor
        implements PriorityOrdered, ApplicationContextAware, BeanFactoryAware, BeanClassLoaderAware {
    /** spring application context */
    private ApplicationContext applicationContext;
    /** spring bean factory */
    private ConfigurableListableBeanFactory beanFactory;
    /** bean class loader */
    private ClassLoader beanClassLoader;

    public KinRpcReferenceFieldProcessor() {
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

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    /**
     * @return reference config bean name
     */
    private String getReferenceConfigBeanName(AnnotationAttributes attributes,
                                              Class<?> interfaceClass) {
        StringJoiner sj = new StringJoiner("$");
        sj.add("Reference");
        sj.add((String) attributes.get("group"));
        sj.add((String) attributes.get("serviceName"));
        sj.add((String) attributes.get("version"));
        return sj.toString();
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes,
                                       Object bean,
                                       String beanName,
                                       Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) {
        if (!injectedType.isInterface()) {
            throw new IllegalArgumentException("annotation '@KinRpcReference' must be used with interface");
        }

        String referenceConfigBeanName = getReferenceConfigBeanName(attributes, injectedType);

        ReferenceConfig<?> referenceConfig = getOrCreateReferenceConfig(attributes, injectedType);
        if (!beanFactory.containsBean(referenceConfigBeanName)) {
            //注册bean
            beanFactory.registerSingleton(referenceConfigBeanName, referenceConfig);
        }
        KinRpcBootstrap.instance().reference(referenceConfig);

        return KinRpcReferenceUtils.createLazyProxy(referenceConfig, beanClassLoader);
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes,
                                                 Object bean,
                                                 String beanName,
                                                 Class<?> injectedType,
                                                 InjectionMetadata.InjectedElement injectedElement) {
        //相当于reference config bean name
        return getReferenceConfigBeanName(attributes, injectedType);
    }

    @Override
    public int getOrder() {
        //最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 解析{@link KinRpcReference}注解, 返回{@link org.kin.kinrpc.config.ReferenceConfig}
     */
    private ReferenceConfig<?> getOrCreateReferenceConfig(AnnotationAttributes attributes,
                                                          Class<?> injectedType) {
        if (!injectedType.isInterface()) {
            throw new IllegalConfigException(String.format("field type is not a interface, KinRpcReference=%s", attributes));
        }
        return KinRpcReferenceUtils.toReferenceConfig(applicationContext, injectedType, attributes);
    }
}
