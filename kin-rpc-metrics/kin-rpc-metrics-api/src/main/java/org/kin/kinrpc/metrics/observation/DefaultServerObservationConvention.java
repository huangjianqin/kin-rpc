package org.kin.kinrpc.metrics.observation;

import io.micrometer.common.KeyValues;

/**
 * default server observation convention
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public class DefaultServerObservationConvention extends AbstractObservationConvention implements ServerObservationConvention {
    /** 单例 */
    private static final DefaultServerObservationConvention INSTANCE = new DefaultServerObservationConvention();

    public static DefaultServerObservationConvention instance() {
        return INSTANCE;
    }

    private DefaultServerObservationConvention() {
    }

    @Override
    public String getName() {
        return "rpc.server.duration";
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerContext context) {
        return super.getLowCardinalityKeyValues(context.getInvocation());
    }

    @Override
    public String getContextualName(ServerContext context) {
        return super.getContextualName(context.getInvocation());
    }
}
