package org.kin.kinrpc.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * 扫描{@link KinRpcReference}注解在interface的scanner配置
 *
 * @author huangjianqin
 * @date 2021/1/17
 */
final class KinRpcReferenceScannerConfiguration implements BeanDefinitionRegistryPostProcessor {
    /** 扫描package classpath的路径集合, 以,分隔 */
    private String basePackage;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //do nothing
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        KinRpcReferenceScanner scanner = new KinRpcReferenceScanner(registry);
        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    //setter && getter
    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}
