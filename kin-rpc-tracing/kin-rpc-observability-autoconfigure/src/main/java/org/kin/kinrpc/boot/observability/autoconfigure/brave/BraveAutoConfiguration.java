package org.kin.kinrpc.boot.observability.autoconfigure.brave;

import brave.SpanCustomizer;
import brave.Tracing;
import brave.TracingCustomizer;
import brave.baggage.*;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.SpanHandler;
import brave.propagation.*;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.*;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.boot.KinRpcProperties;
import org.kin.kinrpc.boot.observability.autoconfigure.KinRpcMicrometerTracingAutoConfiguration;
import org.kin.kinrpc.boot.observability.autoconfigure.ObservabilityUtils;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.kin.kinrpc.config.PropagationConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration")
@AutoConfigureBefore(KinRpcMicrometerTracingAutoConfiguration.class)
@ConditionalOnClass(value = {Tracer.class}, name = {"io.micrometer.tracing.brave.bridge.BraveTracer", "io.micrometer.tracing.brave.bridge.BraveBaggageManager", "brave.Tracing"})
@ConditionalOnTracingEnable
@EnableConfigurationProperties(KinRpcProperties.class)
@Configuration(proxyBeanMethods = false)
public class BraveAutoConfiguration {
    /** 单例 */
    private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

    @Value("${spring.application.name:kinrpc-app}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CompositeSpanHandler compositeSpanHandler(ObjectProvider<SpanExportingPredicate> predicates,
                                              ObjectProvider<SpanReporter> reporters,
                                              ObjectProvider<SpanFilter> filters) {
        return new CompositeSpanHandler(predicates.orderedStream().collect(Collectors.toList()),
                reporters.orderedStream().collect(Collectors.toList()),
                filters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracing braveTracing(KinRpcProperties properties,
                                List<SpanHandler> spanHandlers,
                                List<TracingCustomizer> tracingCustomizers,
                                CurrentTraceContext currentTraceContext,
                                Propagation.Factory propagationFactory,
                                Sampler sampler) {
        String applicationName = properties.getAppName();
        if (StringUtils.isBlank(applicationName)) {
            applicationName = this.applicationName;
        }

        Tracing.Builder builder = Tracing.newBuilder()
                .currentTraceContext(currentTraceContext)
                .traceId128Bit(true)
                .supportsJoin(false)
                .propagationFactory(propagationFactory)
                .sampler(sampler)
                .localServiceName(applicationName);
        spanHandlers.forEach(builder::addSpanHandler);
        for (TracingCustomizer customizer : tracingCustomizers) {
            customizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public brave.Tracer braveTracer(Tracing tracing) {
        return tracing.tracer();
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentTraceContext braveCurrentTraceContext(List<CurrentTraceContext.ScopeDecorator> scopeDecorators,
                                                        List<CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
        ThreadLocalCurrentTraceContext.Builder builder = ThreadLocalCurrentTraceContext.newBuilder();
        scopeDecorators.forEach(builder::addScopeDecorator);
        for (CurrentTraceContextCustomizer customizer : currentTraceContextCustomizers) {
            customizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Sampler braveSampler(KinRpcProperties properties) {
        return Sampler.create(properties.getTracing().getSampling().getProbability());
    }

    /**
     * micrometer与brave的tracing bridge.
     */
    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
        return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
    }

    @Bean
    @ConditionalOnMissingBean
    BravePropagator bravePropagator(brave.Tracing tracing) {
        return new BravePropagator(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(brave.SpanCustomizer.class)
    brave.CurrentSpanCustomizer currentSpanCustomizer(brave.Tracing tracing) {
        return brave.CurrentSpanCustomizer.create(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(SpanCustomizer.class)
    BraveSpanCustomizer braveSpanCustomizer(SpanCustomizer spanCustomizer) {
        return new BraveSpanCustomizer(spanCustomizer);
    }

    /**
     * 定义propagation配置
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = ObservabilityUtils.TRACING_PROPAGATION, havingValue = "false")
    static class BraveNoBaggageConfiguration {
        @Bean
        @ConditionalOnMissingBean
        Propagation.Factory propagationFactory(KinRpcProperties properties) {
            String type = properties.getTracing()
                    .getPropagation()
                    .getType();
            switch (type) {
                case PropagationConfig.B3:
                    return B3Propagation.newFactoryBuilder()
                            .injectFormat(B3Propagation.Format.SINGLE_NO_PARENT)
                            .build();
                case PropagationConfig.W3C:
                    return new W3CPropagation();
                default:
                    throw new IllegalArgumentException("unSupport propagation type");
            }
        }
    }

    @ConditionalOnProperty(name = ObservabilityUtils.TRACING_BAGGAGE_ENABLE, matchIfMissing = true)
    @Configuration(proxyBeanMethods = false)
    static class BraveBaggageConfiguration {
        private final KinRpcProperties properties;

        public BraveBaggageConfiguration(KinRpcProperties properties) {
            this.properties = properties;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.B3)
        BaggagePropagation.FactoryBuilder b3PropagationFactoryBuilder(
                ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            Propagation.Factory delegate = B3Propagation.newFactoryBuilder()
                    .injectFormat(brave.propagation.B3Propagation.Format.SINGLE_NO_PARENT)
                    .build();

            BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream()
                    .forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.W3C, matchIfMissing = true)
        BaggagePropagation.FactoryBuilder w3cPropagationFactoryBuilder(
                ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            Propagation.Factory delegate = new W3CPropagation(BRAVE_BAGGAGE_MANAGER, Collections.emptyList());

            BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream()
                    .forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @Order(0)
        BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer() {
            return (builder) -> {
                List<String> remoteFields = properties.getTracing().getBaggage().getRemoteFields();
                for (String fieldName : remoteFields) {
                    builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean
        Propagation.Factory propagationFactory(BaggagePropagation.FactoryBuilder factoryBuilder) {
            return factoryBuilder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder(
                ObjectProvider<CorrelationScopeCustomizer> correlationScopeCustomizers) {
            CorrelationScopeDecorator.Builder builder = MDCScopeDecorator.newBuilder();
            correlationScopeCustomizers.orderedStream()
                    .forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @Order(0)
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_CORRELATION_ENABLE, matchIfMissing = true)
        CorrelationScopeCustomizer correlationFieldsCorrelationScopeCustomizer() {
            return (builder) -> {
                List<String> correlationFields = properties.getTracing()
                        .getBaggage()
                        .getCorrelation()
                        .getFields();
                for (String field : correlationFields) {
                    builder.add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageField.create(field))
                            .flushOnUpdate().build());
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean(CurrentTraceContext.ScopeDecorator.class)
        CurrentTraceContext.ScopeDecorator correlationScopeDecorator(CorrelationScopeDecorator.Builder builder) {
            return builder.build();
        }
    }
}
