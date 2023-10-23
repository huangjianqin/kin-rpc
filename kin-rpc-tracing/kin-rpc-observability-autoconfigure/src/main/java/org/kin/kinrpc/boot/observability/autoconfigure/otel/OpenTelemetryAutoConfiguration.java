package org.kin.kinrpc.boot.observability.autoconfigure.otel;

import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import io.micrometer.tracing.otel.bridge.*;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.boot.KinRpcProperties;
import org.kin.kinrpc.boot.observability.autoconfigure.KinRpcMicrometerTracingAutoConfiguration;
import org.kin.kinrpc.boot.observability.autoconfigure.ObservabilityUtils;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.kin.kinrpc.common.Version;
import org.kin.kinrpc.config.PropagationConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration")
@AutoConfigureBefore(KinRpcMicrometerTracingAutoConfiguration.class)
@ConditionalOnTracingEnable
@ConditionalOnClass(name = {"io.micrometer.tracing.otel.bridge.OtelTracer",
        "io.opentelemetry.sdk.trace.SdkTracerProvider",
        "io.opentelemetry.api.OpenTelemetry",
        "io.micrometer.tracing.SpanCustomizer"})
@EnableConfigurationProperties(KinRpcProperties.class)
public class OpenTelemetryAutoConfiguration {
    @Autowired
    private KinRpcProperties properties;
    @Value("${spring.application.name:kinrpc-app}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(OpenTelemetry.class)
    OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider,
                                ContextPropagators contextPropagators) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    SdkTracerProvider otelSdkTracerProvider(ObjectProvider<SpanProcessor> spanProcessors,
                                            Sampler sampler) {
        String applicationName = properties.getAppName();
        if (StringUtils.isBlank(applicationName)) {
            applicationName = this.applicationName;
        }
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setSampler(sampler)
                .setResource(Resource.create(
                        Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
        spanProcessors.orderedStream()
                .forEach(builder::addSpanProcessor);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    ContextPropagators otelContextPropagators(ObjectProvider<TextMapPropagator> textMapPropagators) {
        return ContextPropagators.create(
                TextMapPropagator.composite(
                        textMapPropagators.orderedStream().
                                collect(Collectors.toList())));
    }

    @Bean
    @ConditionalOnMissingBean
    Sampler otelSampler() {
        Sampler rootSampler = Sampler.traceIdRatioBased(this.properties.getTracing().getSampling().getProbability());
        return Sampler.parentBased(rootSampler);
    }

    @Bean
    @ConditionalOnMissingBean
    SpanProcessor otelSpanProcessor(ObjectProvider<SpanExporter> spanExporters,
                                    ObjectProvider<SpanExportingPredicate> spanExportingPredicates,
                                    ObjectProvider<SpanReporter> spanReporters,
                                    ObjectProvider<SpanFilter> spanFilters) {
        return BatchSpanProcessor.builder(new CompositeSpanExporter(spanExporters.orderedStream().collect(Collectors.toList()),
                spanExportingPredicates.orderedStream().collect(Collectors.toList()),
                spanReporters.orderedStream().collect(Collectors.toList()),
                spanFilters.orderedStream().collect(Collectors.toList()))).build();
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("kin.kinrpc", Version.getVersion());
    }

    /**
     * micrometer与otel的tracing bridge.
     */
    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    OtelTracer otelTracerBridge(io.opentelemetry.api.trace.Tracer tracer,
                                OtelTracer.EventPublisher eventPublisher,
                                OtelCurrentTraceContext otelCurrentTraceContext) {
        return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
                new OtelBaggageManager(otelCurrentTraceContext,
                        this.properties.getTracing().getBaggage().getRemoteFields(),
                        Collections.emptyList()));
    }

    @Bean
    @ConditionalOnMissingBean
    OtelPropagator otelPropagator(ContextPropagators contextPropagators, io.opentelemetry.api.trace.Tracer tracer) {
        return new OtelPropagator(contextPropagators, tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    OtelTracer.EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
        return new MultiOtelEventPublisher(eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    OtelCurrentTraceContext otelCurrentTraceContext(OtelTracer.EventPublisher publisher) {
        ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
        return new OtelCurrentTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    Slf4JEventListener otelSlf4JEventListener() {
        return new Slf4JEventListener();
    }

    @Bean
    @ConditionalOnMissingBean(SpanCustomizer.class)
    OtelSpanCustomizer otelSpanCustomizer() {
        return new OtelSpanCustomizer();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ObservabilityUtils.TRACING_BAGGAGE_ENABLE, matchIfMissing = true)
    static class BaggageConfiguration {
        private final KinRpcProperties properties;

        BaggageConfiguration(KinRpcProperties properties) {
            this.properties = properties;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.W3C,
                matchIfMissing = true)
        TextMapPropagator w3cTextMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.properties.getTracing().getBaggage().getRemoteFields();
            return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance(),
                    new BaggageTextMapPropagator(remoteFields,
                            new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.B3)
        TextMapPropagator b3BaggageTextMapPropagator(OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.properties.getTracing().getBaggage().getRemoteFields();
            return TextMapPropagator.composite(B3Propagator.injectingSingleHeader(),
                    new BaggageTextMapPropagator(remoteFields,
                            new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_CORRELATION_ENABLE, matchIfMissing = true)
        Slf4JBaggageEventListener otelSlf4JBaggageEventListener() {
            return new Slf4JBaggageEventListener(this.properties.getTracing().getBaggage().getCorrelation().getFields());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ObservabilityUtils.TRACING_BAGGAGE_ENABLE, havingValue = "false")
    static class NoBaggageConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.B3)
        B3Propagator b3TextMapPropagator() {
            return B3Propagator.injectingSingleHeader();
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = ObservabilityUtils.TRACING_PROPAGATION_TYPE, havingValue = PropagationConfig.W3C,
                matchIfMissing = true)
        W3CTraceContextPropagator w3cTextMapPropagatorWithoutBaggage() {
            return W3CTraceContextPropagator.getInstance();
        }

    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /** 支持同时触发多个event listener */
    static class MultiOtelEventPublisher implements OtelTracer.EventPublisher {
        /** otel event handler */
        private final List<EventListener> listeners;

        MultiOtelEventPublisher(List<EventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void publishEvent(Object event) {
            for (EventListener listener : this.listeners) {
                listener.onEvent(event);
            }
        }

    }
}
