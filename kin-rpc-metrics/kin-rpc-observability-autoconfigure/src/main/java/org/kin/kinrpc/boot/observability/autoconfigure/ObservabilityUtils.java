package org.kin.kinrpc.boot.observability.autoconfigure;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
public final class ObservabilityUtils {
    public static final String TRACING_PROPAGATION = "kinrpc.tracing.propagation";
    public static final String TRACING_PROPAGATION_TYPE = TRACING_PROPAGATION + ".type";
    public static final String TRACING_PROPAGATION_CORRELATION = TRACING_PROPAGATION + ".correlation";
    public static final String TRACING_PROPAGATION_CORRELATION_ENABLE = TRACING_PROPAGATION_CORRELATION + ".enable";


    public static final String TRACING_BAGGAGE = "kinrpc.tracing.baggage";
    public static final String TRACING_BAGGAGE_ENABLE = TRACING_BAGGAGE + ".enable";


    /** otlp config key前缀 */
    public static final String TRACING_TRACING_EXPORTER_OTLP_CONFIG = "kinrpc.tracing.exporter.otlp";


    /** zipkin config key前缀 */
    public static final String TRACING_TRACING_EXPORTER_ZIPKIN_CONFIG = "kinrpc.tracing.exporter.zipkin";

    private ObservabilityUtils() {
    }
}
