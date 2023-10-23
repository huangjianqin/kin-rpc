package org.kin.kinrpc.boot.observability.autoconfigure.exporter.otlp;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.kin.kinrpc.boot.KinRpcProperties;
import org.kin.kinrpc.boot.observability.autoconfigure.ObservabilityUtils;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.kin.kinrpc.config.OtlpConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
@ConditionalOnClass(name = {"io.micrometer.tracing.otel.bridge.OtelTracer",
        "io.opentelemetry.sdk.trace.SdkTracerProvider",
        "io.opentelemetry.api.OpenTelemetry",
        "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter"})
@ConditionalOnTracingEnable
@EnableConfigurationProperties(KinRpcProperties.class)
@Configuration(proxyBeanMethods = false)
public class OtlpSenderAutoConfiguration {
    /**
     * default grpc span exporter
     */
    @Bean
    @ConditionalOnProperty(prefix = ObservabilityUtils.TRACING_TRACING_EXPORTER_OTLP_CONFIG, name = "endpoint")
    @ConditionalOnMissingBean(type = {"io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter",
            "io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter"})
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter(KinRpcProperties properties) {
        OtlpConfig cfg = properties.getTracing().getExporter().getOtlp();
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(cfg.getEndpoint())
                .setTimeout(cfg.getTimeout())
                .setCompression(cfg.getCompressionMethod());
        for (Map.Entry<String, String> entry : cfg.getHeaders().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
