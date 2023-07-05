package org.kin.kinrpc.boot;

import org.kin.framework.utils.CollectionUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于构建扫描{@link KinRpcReference}注解在interface的scanner配置
 *
 * @author huangjianqin
 * @date 2021/1/17
 */
final class KinRpcReferenceRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes scanAttrs = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(EnableKinRpcReference.class.getName()));
        registerBeanDefinitions(importingClassMetadata, registry, scanAttrs,
                importingClassMetadata.getClassName() + "#" + EnableKinRpcReferencePostProcessor.class.getSimpleName());
    }

    /**
     * 注册KinRpcReferenceScannerConfiguration bean
     */
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, AnnotationAttributes scanAttrs, String beanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(EnableKinRpcReferencePostProcessor.class);
        List<String> basePackages = new ArrayList<>();

        String[] basePackageArr = scanAttrs.getStringArray("scanBasePackages");
        if (CollectionUtils.isNonEmpty(basePackageArr)) {
            //过滤空串
            basePackages.addAll(Arrays.stream(basePackageArr).filter(StringUtils::hasText).collect(Collectors.toList()));
            //如果是class name, 则取其package class path
            basePackages.addAll(Arrays.stream(basePackageArr).map(ClassUtils::getPackageName).collect(Collectors.toList()));
        }
        //@Import(KinRpcReferenceRegistrar.class)的声明类package class path
        basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));

        builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));

        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }
}
