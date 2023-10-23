package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.customizer;

import org.kin.kinrpc.config.ZipkinConfig;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * user custom {@link WebClient.Builder}
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@FunctionalInterface
public interface ZipkinWebClientBuilderCustomizer {
    /**
     * user custom {@code webClientBuilder}
     *
     * @param zipkinConfig     zipkin exporter config
     * @param webClientBuilder the {@code WebClient.Builder} to customize
     */
    void customize(ZipkinConfig zipkinConfig, WebClient.Builder webClientBuilder);

}