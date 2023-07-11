package org.kin.kinrpc.boot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huangjianqin
 * @date 2020/12/13
 */
@ConditionalOnBean(KinRpcMarkerConfiguration.Marker.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({KinRpcProperties.class})
public class KinRpcAutoConfiguration {

    @Bean
    public ConfigBeanPostProcessor configBeanPostProcessor() {
        return new ConfigBeanPostProcessor();
    }

    @Bean
    public KinRpcServiceBeanProcessor kinRpcServiceBeanProcessor() {
        return new KinRpcServiceBeanProcessor();
    }

    @Bean
    public KinRpcBootstrapApplicationListener kinRpcBootstrapApplicationListener() {
        return new KinRpcBootstrapApplicationListener();
    }

    @Bean
    public KinRpcReferenceFieldProcessor kinRpcReferenceFieldProcessor() {
        return new KinRpcReferenceFieldProcessor();
    }
}
