package org.kin.kinrpc.boot.skywalking;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingDefaultTracingHandler;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingReceiverTracingHandler;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingSenderTracingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
@AutoConfigureAfter(name = {"org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration"})
@ConditionalOnClass(name = {"org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingSenderTracingHandler",
        "org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingReceiverTracingHandler",
        "org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingDefaultTracingHandler"})
@ConditionalOnProperty(prefix = "kinrpc.tracing", name = "enabled")
@Configuration(proxyBeanMethods = false)
public class SkywalkingAutoConfiguration implements BeanFactoryAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(SkywalkingAutoConfiguration.class);

    /** bean factory */
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            ObservationRegistry observationRegistry = beanFactory.getBean(ObservationRegistry.class);
            observationRegistry.observationConfig()
                    .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                            new SkywalkingSenderTracingHandler(), new SkywalkingReceiverTracingHandler(),
                            new SkywalkingDefaultTracingHandler()
                    ));

            org.kin.kinrpc.ApplicationContext.instance()
                    .getBeanFactory()
                    .registerBean(observationRegistry);
        } catch (NoSuchBeanDefinitionException e) {
            log.info("please use a version of micrometer higher than 1.10.0 ：{}", e.getMessage());
        }
    }

    /**
     * default observation registry
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(value = ObservationRegistry.class)
    ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}
