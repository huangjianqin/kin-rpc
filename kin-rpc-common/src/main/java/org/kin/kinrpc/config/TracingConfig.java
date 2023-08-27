package org.kin.kinrpc.config;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class TracingConfig extends AbstractConfig {
    /** 是否启动tracing */
    private Boolean enabled = true;
    /** sampling config */
    private SamplingConfig sampling = DefaultConfig.DEFAULT_SAMPLING_CONFIG;
    /** baggage config */
    private BaggageConfig baggage = DefaultConfig.DEFAULT_BAGGAGE_CONFIG;
    /** propagation config */
    private PropagationConfig propagation = DefaultConfig.DEFAULT_PROPAGATION_CONFIG;
    /** exporter config */
    private ExporterConfig exporter;

    public TracingConfig enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public TracingConfig sampling(SamplingConfig sampling) {
        this.sampling = sampling;
        return this;
    }

    public TracingConfig baggage(BaggageConfig baggageConfig) {
        this.baggage = baggageConfig;
        return this;
    }

    public TracingConfig propagation(PropagationConfig propagation) {
        this.propagation = propagation;
        return this;
    }

    public TracingConfig exporter(ExporterConfig exporter) {
        this.exporter = exporter;
        return this;
    }

    //setter && getter
    public boolean isEnabled() {
        return Objects.nonNull(enabled) && enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public SamplingConfig getSampling() {
        return sampling;
    }

    public void setSampling(SamplingConfig sampling) {
        this.sampling = sampling;
    }

    public BaggageConfig getBaggage() {
        return baggage;
    }

    public void setBaggage(BaggageConfig baggage) {
        this.baggage = baggage;
    }

    public PropagationConfig getPropagation() {
        return propagation;
    }

    public void setPropagation(PropagationConfig propagation) {
        this.propagation = propagation;
    }

    public ExporterConfig getExporter() {
        return exporter;
    }

    public void setExporter(ExporterConfig exporter) {
        this.exporter = exporter;
    }

    @Override
    public String toString() {
        return "TracingConfig{" +
                "enabled=" + enabled +
                ", sampling=" + sampling +
                ", baggage=" + baggage +
                ", propagation=" + propagation +
                ", exporter=" + exporter +
                "}";
    }
}
