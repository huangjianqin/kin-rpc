package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ExporterConfig extends AbstractConfig {
    /** zipkin exporter config */
    private ZipkinConfig zipkin;
    /** otlp exporter config */
    private OtlpConfig otlp;

    public ExporterConfig zipkinConfig(ZipkinConfig zipkin) {
        this.zipkin = zipkin;
        return this;
    }

    public ExporterConfig otlpConfig(OtlpConfig otlp) {
        this.otlp = otlp;
        return this;
    }

    //setter && getter
    public ZipkinConfig getZipkin() {
        return zipkin;
    }

    public void setZipkin(ZipkinConfig zipkin) {
        this.zipkin = zipkin;
    }

    public OtlpConfig getOtlp() {
        return otlp;
    }

    public void setOtlp(OtlpConfig otlp) {
        this.otlp = otlp;
    }

    @Override
    public String toString() {
        return "{" +
                "zipkin=" + zipkin +
                ", otlp=" + otlp +
                "}";
    }
}
