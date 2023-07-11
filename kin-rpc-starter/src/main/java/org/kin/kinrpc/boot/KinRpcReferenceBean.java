package org.kin.kinrpc.boot;

import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.ReferenceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.MethodMetadata;

import java.util.Map;
import java.util.Objects;

/**
 * {@link KinRpcReference} + {@link org.springframework.context.annotation.Bean}形式构建service reference
 *
 * @author huangjianqin
 * @date 2023/7/10
 */
public final class KinRpcReferenceBean<T> implements FactoryBean<T>, BeanFactoryAware,
        BeanNameAware, InitializingBean, ApplicationContextAware, BeanClassLoaderAware {
    private final Class<T> interfaceClass;
    /** bean name */
    private String beanName;
    /** spring application context */
    private ApplicationContext applicationContext;
    /** bean factory */
    private ConfigurableListableBeanFactory beanFactory;
    /** bean class loader */
    private ClassLoader beanClassLoader;
    /** reference config */
    private ReferenceConfig<T> referenceConfig;
    private T lazyProxy;

    public KinRpcReferenceBean(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public T getObject() {
        if (Objects.isNull(lazyProxy)) {
            lazyProxy = KinRpcReferenceUtils.createLazyProxy(referenceConfig, beanClassLoader);
        }
        return lazyProxy;
    }

    @Override
    public Class<?> getObjectType() {
        return referenceConfig.getInterfaceClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {
        //获取BeanDefinition
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (!(beanDefinition instanceof AnnotatedBeanDefinition)) {
            return;
        }
        AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
        //获取@KinRpcReference注解属性
        MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
        if (Objects.isNull(factoryMethodMetadata)) {
            throw new IllegalArgumentException(String.format("unavailable to create %s instance without @Bean", KinRpcReferenceBean.class.getName()));
        }
        Map<String, Object> annoAttrsMap = factoryMethodMetadata.getAnnotationAttributes(KinRpcReference.class.getName());
        if (Objects.isNull(annoAttrsMap)) {
            throw new IllegalArgumentException("bean factory method is not annotated with @" + KinRpcReference.class.getName());
        }

        referenceConfig = KinRpcReferenceUtils.toReferenceConfig(applicationContext, interfaceClass, annoAttrsMap);
        KinRpcBootstrap.instance().reference(referenceConfig);
    }
}
