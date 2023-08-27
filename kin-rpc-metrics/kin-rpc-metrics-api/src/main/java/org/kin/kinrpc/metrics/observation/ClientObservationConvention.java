package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * client observation convention
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public interface ClientObservationConvention extends ObservationConvention<ClientContext> {
    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ClientContext;
    }
}
