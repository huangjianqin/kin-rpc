package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * server observation convention
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public interface ServerObservationConvention extends ObservationConvention<ServerContext> {
    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ServerContext;
    }
}
