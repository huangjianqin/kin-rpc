package org.kin.kinrpc.spring;

import org.kin.framework.log.LoggerOprs;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * 扫描{@link KinRpcReference}注解在interface的所有beanDefinitions
 *
 * @author huangjianqin
 * @date 2021/1/17
 */
final class KinRpcReferenceScanner extends ClassPathBeanDefinitionScanner implements LoggerOprs {
    public KinRpcReferenceScanner(BeanDefinitionRegistry registry) {
        super(registry);
        registerFilters();
    }

    /**
     * 注册filter, 指定classpath扫描策略
     */
    private void registerFilters() {
        // 扫描@KinRpcReference注解的top-level class
        addIncludeFilter(new AnnotationTypeFilter(KinRpcReference.class));

        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.isEmpty()) {
            warn("No KinRpc reference interface was found in classpath. Please check your configuration.");
        } else {
            processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions;
    }

    /**
     * 处理扫描到的KinRpc reference interface bean
     */
    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        for (BeanDefinitionHolder holder : beanDefinitions) {
            GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
            String beanClassName = definition.getBeanClassName();
            //factory bean constructor args
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            //factory bean class
            definition.setBeanClass(KinRpcReferenceFactoryBean.class);
            //enable autowire
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            //set lazy init
            definition.setLazyInit(true);
        }
    }

    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        } else {
            warn("Skipping KinRpcReferenceFactoryBean with name '" + beanName + "' and '"
                    + beanDefinition.getBeanClassName() + "' ReferenceInterface" + ". Bean already defined with the same name!");
            return false;
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        //@KinRpcReference && 接口 && top-level class, 即pulic class 或者 内部static类
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.getAnnotationTypes().contains(KinRpcReference.class.getName()) &&
                metadata.isInterface() &&
                metadata.isIndependent();
    }
}
