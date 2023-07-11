package org.kin.kinrpc.boot;

import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动纳管所有注册到spring容器的config bean
 *
 * @author huangjianqin
 * @date 2023/7/10
 */
public class ConfigBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        KinRpcBootstrap bootstrap = KinRpcBootstrap.instance();
        if (bean instanceof RegistryConfig) {
            bootstrap.registry((RegistryConfig) bean);
        }

        if (bean instanceof ServerConfig) {
            bootstrap.servers((ServerConfig) bean);
        }

        if (bean instanceof ExecutorConfig) {
            bootstrap.executor((ExecutorConfig) bean);
        }

        if (bean instanceof ProviderConfig) {
            bootstrap.providers((ProviderConfig) bean);
        }

        if (bean instanceof ConsumerConfig) {
            bootstrap.consumer((ConsumerConfig) bean);
        }

        if (bean instanceof ServiceConfig) {
            bootstrap.service((ServiceConfig<?>) bean);
        }

        if (bean instanceof ReferenceConfig) {
            bootstrap.reference((ReferenceConfig<?>) bean);
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
