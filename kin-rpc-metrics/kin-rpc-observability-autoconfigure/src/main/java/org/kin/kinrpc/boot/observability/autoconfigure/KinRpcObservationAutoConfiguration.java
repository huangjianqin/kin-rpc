package org.kin.kinrpc.boot.observability.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.kin.kinrpc.ApplicationContext;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 注册{@link io.micrometer.observation.ObservationRegistry}
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@AutoConfigureAfter(value = KinRpcMicrometerTracingAutoConfiguration.class, name = "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration")
@ConditionalOnTracingEnable
@ConditionalOnClass({Observation.class, Tracer.class})
@Configuration(proxyBeanMethods = false)
public class KinRpcObservationAutoConfiguration implements BeanFactoryAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(KinRpcObservationAutoConfiguration.class);

    /** bean factory */
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            ApplicationContext.instance()
                    .getBeanFactory()
                    .registerBean(beanFactory.getBean(ObservationRegistry.class));
            ApplicationContext.instance()
                    .getBeanFactory()
                    .registerBean(beanFactory.getBean(Tracer.class));
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

    /**
     * default observation registry post processor
     */
    @Bean
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    @ConditionalOnClass(value = ObservationHandler.class)
    public ObservationRegistryPostProcessor kinRpcObservationRegistryPostProcessor(ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
                                                                                   ObjectProvider<ObservationHandler<?>> observationHandlers) {
        return new ObservationRegistryPostProcessor(observationHandlerGrouping, observationHandlers);
    }

    /**
     * 仅仅metrics
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyMetricsConfiguration {

        @Bean
        @ConditionalOnClass(name = "io.micrometer.core.instrument.observation.MeterObservationHandler")
        ObservationHandlerGrouping metricsObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(MeterObservationHandler.class);
        }

    }

    /**
     * 仅仅tracing
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyTracingConfiguration {

        @Bean
        @ConditionalOnClass(TracingObservationHandler.class)
        ObservationHandlerGrouping tracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(TracingObservationHandler.class);
        }

    }

    /**
     * metrics + tracing
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({MeterRegistry.class, Tracer.class})
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class MetricsWithTracingConfiguration {

        @Bean
        @ConditionalOnClass(value = {TracingObservationHandler.class, MeterObservationHandler.class})
        ObservationHandlerGrouping metricsAndTracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(
                    Arrays.asList(TracingObservationHandler.class,
                            MeterObservationHandler.class));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(MeterObservationHandler.class)
    static class MeterObservationHandlerConfiguration {

        @ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
        @Configuration(proxyBeanMethods = false)
        static class OnlyMetricsMeterObservationHandlerConfiguration {

            /** default metrics meter observation handler */
            @Bean
            @ConditionalOnClass(value = DefaultMeterObservationHandler.class)
            DefaultMeterObservationHandler defaultMeterObservationHandler(MeterRegistry meterRegistry) {
                return new DefaultMeterObservationHandler(meterRegistry);
            }

        }

        @ConditionalOnBean(Tracer.class)
        @Configuration(proxyBeanMethods = false)
        static class TracingAndMetricsObservationHandlerConfiguration {

            /** default tracing observation handler */
            @Bean
            @ConditionalOnClass(value = {TracingAwareMeterObservationHandler.class, Tracer.class})
            TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
                    MeterRegistry meterRegistry, Tracer tracer) {
                DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
                return new TracingAwareMeterObservationHandler<>(delegate, tracer);
            }

        }

    }
}
