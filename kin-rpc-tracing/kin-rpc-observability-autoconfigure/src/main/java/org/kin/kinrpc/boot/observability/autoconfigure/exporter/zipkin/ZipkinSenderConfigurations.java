package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.kin.kinrpc.boot.KinRpcProperties;
import org.kin.kinrpc.boot.observability.autoconfigure.ObservabilityUtils;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.customizer.ZipkinRestTemplateBuilderCustomizer;
import org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.customizer.ZipkinWebClientBuilderCustomizer;
import org.kin.kinrpc.config.ZipkinConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
class ZipkinSenderConfigurations {
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ObservabilityUtils.TRACING_TRACING_EXPORTER_ZIPKIN_CONFIG, name = "endpoint")
    @Import({UrlConnectionSenderConfiguration.class,
            WebClientSenderConfiguration.class,
            RestTemplateSenderConfiguration.class})
    static class SenderConfiguration {
    }

    /**
     * zipkin自带url connection sender
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "zipkin2.reporter.urlconnection.URLConnectionSender")
    @EnableConfigurationProperties(KinRpcProperties.class)
    static class UrlConnectionSenderConfiguration {
        @Bean
        @ConditionalOnMissingBean(Sender.class)
        URLConnectionSender urlConnectionSender(KinRpcProperties properties) {
            URLConnectionSender.Builder builder = URLConnectionSender.newBuilder();
            ZipkinConfig zipkinConfig = properties.getTracing().getExporter().getZipkin();
            builder.connectTimeout((int) zipkinConfig.getConnectTimeout().toMillis());
            builder.readTimeout((int) zipkinConfig.getReadTimeout().toMillis());
            builder.endpoint(zipkinConfig.getEndpoint());
            return builder.build();
        }

    }

    /**
     * 基于spring webmvc http client的zipkin sender
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
    @EnableConfigurationProperties(KinRpcProperties.class)
    static class RestTemplateSenderConfiguration {
        @Bean
        @ConditionalOnMissingBean(Sender.class)
        ZipkinRestTemplateSender restTemplateSender(KinRpcProperties properties,
                                                    ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
            ZipkinConfig zipkinConfig = properties.getTracing().getExporter().getZipkin();
            RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder()
                    .setConnectTimeout(zipkinConfig.getConnectTimeout())
                    .setReadTimeout(zipkinConfig.getReadTimeout());
            restTemplateBuilder = applyCustomizers(restTemplateBuilder, customizers);
            return new ZipkinRestTemplateSender(zipkinConfig.getEndpoint(), restTemplateBuilder.build());
        }

        private RestTemplateBuilder applyCustomizers(RestTemplateBuilder restTemplateBuilder,
                                                     ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
            Iterable<ZipkinRestTemplateBuilderCustomizer> orderedCustomizers = () -> customizers.orderedStream()
                    .iterator();
            RestTemplateBuilder currentBuilder = restTemplateBuilder;
            for (ZipkinRestTemplateBuilderCustomizer customizer : orderedCustomizers) {
                currentBuilder = customizer.customize(currentBuilder);
            }
            return currentBuilder;
        }

    }

    /**
     * 基于spring webflux http client的zipkin sender
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    @EnableConfigurationProperties(KinRpcProperties.class)
    static class WebClientSenderConfiguration {
        @Bean
        @ConditionalOnMissingBean(Sender.class)
        ZipkinWebClientSender webClientSender(KinRpcProperties properties,
                                              ObjectProvider<ZipkinWebClientBuilderCustomizer> customizers) {
            ZipkinConfig zipkinConfig = properties.getTracing().getExporter().getZipkin();
            WebClient.Builder builder = WebClient.builder();
            customizers.orderedStream().forEach((customizer) -> customizer.customize(zipkinConfig, builder));
            return new ZipkinWebClientSender(zipkinConfig.getEndpoint(), builder.build());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ReporterConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(Sender.class)
        AsyncReporter<Span> spanReporter(Sender sender, BytesEncoder<Span> encoder) {
            return AsyncReporter.builder(sender).build(encoder);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ZipkinSpanHandler.class)
    static class BraveConfiguration {
        @Bean
        @ConditionalOnMissingBean(ZipkinSpanHandler.class)
        @ConditionalOnBean(Reporter.class)
        ZipkinSpanHandler zipkinSpanHandler(Reporter<Span> spanReporter) {
            return (ZipkinSpanHandler) ZipkinSpanHandler.newBuilder(spanReporter).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.opentelemetry.exporter.zipkin.ZipkinSpanExporter")
    @ConditionalOnProperty(prefix = ObservabilityUtils.TRACING_TRACING_EXPORTER_ZIPKIN_CONFIG, name = "endpoint")
    @EnableConfigurationProperties(KinRpcProperties.class)
    static class OpenTelemetryConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "io.opentelemetry.exporter.zipkin.ZipkinSpanExporter")
        ZipkinSpanExporter zipkinSpanExporter(KinRpcProperties properties,
                                              BytesEncoder<Span> encoder,
                                              ObjectProvider<Sender> senders) {
            ZipkinConfig zipkinConfig = properties.getTracing().getExporter().getZipkin();

            AtomicReference<Sender> senderRef = new AtomicReference<>();
            senders.orderedStream()
                    .findFirst()
                    .ifPresent(senderRef::set);
            Sender sender = senderRef.get();
            if (sender == null) {
                return ZipkinSpanExporter.builder()
                        .setEncoder(encoder)
                        .setEndpoint(zipkinConfig.getEndpoint())
                        .setReadTimeout(zipkinConfig.getReadTimeout())
                        .build();
            }
            return ZipkinSpanExporter.builder()
                    .setEncoder(encoder)
                    .setSender(sender)
                    .build();
        }

    }
}
