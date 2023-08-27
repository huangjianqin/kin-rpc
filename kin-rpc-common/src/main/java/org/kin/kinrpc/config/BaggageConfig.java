package org.kin.kinrpc.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class BaggageConfig extends AbstractConfig {
    private Boolean enabled = true;
    /** correlation configuration */
    private Correlation correlation = DefaultConfig.DEFAULT_BAGGAGE_CORRELATION;
    /**
     * List of fields that are referenced the same in-process as it is on the wire
     * For example, the field "x-vcap-request-id" would be set as-is including the prefix
     */
    private List<String> remoteFields = new ArrayList<>();

    public BaggageConfig enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public BaggageConfig correlation(Correlation correlation) {
        this.correlation = correlation;
        return this;
    }

    public BaggageConfig remoteFields(List<String> remoteFields) {
        this.remoteFields = remoteFields;
        return this;
    }

    public BaggageConfig remoteFields(String... remoteFields) {
        return remoteFields(Arrays.asList(remoteFields));
    }

    //setter && getter
    public boolean isEnabled() {
        return Objects.nonNull(enabled) & enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Correlation getCorrelation() {
        return correlation;
    }

    public void setCorrelation(Correlation correlation) {
        this.correlation = correlation;
    }

    public List<String> getRemoteFields() {
        return remoteFields;
    }

    public void setRemoteFields(List<String> remoteFields) {
        this.remoteFields = remoteFields;
    }

    //-------------------------------------------------------------------------------------------
    public static class Correlation implements Serializable {
        /** Whether to enable correlation of the baggage context with logging contexts */
        private boolean enabled = true;
        /**
         * List of fields that should be correlated with the logging context. That
         * means that these fields would end up as key-value pairs in e.g. MDC.
         */
        private List<String> fields = new ArrayList<>();

        public Correlation fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        public Correlation fields(String... fields) {
            return fields(Arrays.asList(fields));
        }

        //setter && getter
        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getFields() {
            return this.fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }
    }
}
