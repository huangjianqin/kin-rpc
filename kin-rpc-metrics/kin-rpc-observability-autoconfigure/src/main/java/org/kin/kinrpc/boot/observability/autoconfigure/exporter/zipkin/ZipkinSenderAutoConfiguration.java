package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin;

import org.kin.kinrpc.boot.KinRpcProperties;
import org.kin.kinrpc.boot.observability.autoconfigure.ObservabilityUtils;
import org.kin.kinrpc.boot.observability.autoconfigure.annotation.ConditionalOnTracingEnable;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.ZipkinSenderConfigurations.BraveConfiguration;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.ZipkinSenderConfigurations.OpenTelemetryConfiguration;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.ZipkinSenderConfigurations.ReporterConfiguration;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.ZipkinSenderConfigurations.SenderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.SpanBytesEncoder;

/**
 * zipkin配置
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@AutoConfigureAfter(value = RestTemplateAutoConfiguration.class, name = "org.springframework.boot.actuate.autoconfigure.tracing.zipkin")
@ConditionalOnClass(name = "zipkin2.reporter.Sender")
@ConditionalOnTracingEnable
@Import({SenderConfiguration.class,
        ReporterConfiguration.class,
        BraveConfiguration.class,
        OpenTelemetryConfiguration.class})
@EnableConfigurationProperties(KinRpcProperties.class)
@Configuration(proxyBeanMethods = false)
public class ZipkinSenderAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = ObservabilityUtils.TRACING_TRACING_EXPORTER_ZIPKIN_CONFIG, name = "endpoint")
    @ConditionalOnMissingBean
    public BytesEncoder<Span> spanBytesEncoder() {
        return SpanBytesEncoder.JSON_V2;
    }
}
