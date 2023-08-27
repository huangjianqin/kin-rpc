package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin.customizer;

import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * user custom {@link RestTemplateBuilder}
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@FunctionalInterface
public interface ZipkinRestTemplateBuilderCustomizer {

    /**
     * user custom {@code  restTemplateBuilder}
     *
     * @param restTemplateBuilder the {@code RestTemplateBuilder} to customize
     * @return the customized {@code RestTemplateBuilder}
     */
    RestTemplateBuilder customize(RestTemplateBuilder restTemplateBuilder);
}
