package org.kin.kinrpc.boot.observability.autoconfigure;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 注册tracing observation handler beans
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@ConditionalOnTracingEnable
@ConditionalOnClass(value = {Observation.class, Tracer.class, Propagator.class})
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration")
@Configuration(proxyBeanMethods = false)
public class KinRpcMicrometerTracingAutoConfiguration {
    /**
     * {@code @Order} value of
     * {@link #propagatingReceiverTracingObservationHandler(Tracer, Propagator)}
     */
    public static final int RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

    /**
     * {@code @Order} value of
     * {@link #propagatingSenderTracingObservationHandler(Tracer, Propagator)}
     */
    public static final int SENDER_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

    /** default tracing observation handler */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(io.micrometer.tracing.Tracer.class)
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    /** propagating sender tracing observation handler */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracer.class, Propagator.class})
    @Order(SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
    public PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(Tracer tracer,
                                                                                                    Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
    }

    /** propagating receiver tracing observation handler */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracer.class, Propagator.class})
    @Order(RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
    public PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(Tracer tracer,
                                                                                                        Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
    }
}
