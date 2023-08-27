package org.kin.kinrpc.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
public class OtlpConfig extends AbstractConfig {
    /** otlp endpoint */
    private String endpoint;
    /** the maximum time to wait for the collector to process an exported batch of spans */
    private Duration timeout = DefaultConfig.DEFAULT_TRACING_EXPORTER_OTLP_TIMEOUT;
    /**
     * The method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include "gzip" and "none".
     */
    private String compressionMethod = DefaultConfig.DEFAULT_TRACING_EXPORTER_OTLP_COMPRESSION_METHOD;
    /** request header, grpc or http */
    private Map<String, String> headers = new HashMap<>();

    public OtlpConfig endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public OtlpConfig timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public OtlpConfig compressionMethod(String compressionMethod) {
        this.compressionMethod = compressionMethod;
        return this;
    }

    public OtlpConfig headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    //setter && getter
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getCompressionMethod() {
        return compressionMethod;
    }

    public void setCompressionMethod(String compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "{" +
                "endpoint='" + endpoint + '\'' +
                ", timeout=" + timeout +
                ", compressionMethod='" + compressionMethod + '\'' +
                ", headers=" + headers +
                '}';
    }
}
