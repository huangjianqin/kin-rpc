package org.kin.kinrpc.config;

import java.time.Duration;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
public class ZipkinConfig extends AbstractConfig {
    /** zipkin API, 比如http://localhost:9411/api/v2/spans */
    private String endpoint;
    /** connection timeout for requests to zipkin */
    private Duration connectTimeout = DefaultConfig.DEFAULT_TRACING_EXPORTER_ZIPKIN_CONNECT_TIMEOUT;
    /** read timeout for requests to zipkin */
    private Duration readTimeout = DefaultConfig.DEFAULT_TRACING_EXPORTER_OTLP_TIMEOUT;

    public ZipkinConfig endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ZipkinConfig connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public ZipkinConfig readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    //setter && getter
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public String toString() {
        return "{" +
                "endpoint='" + endpoint + '\'' +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}
