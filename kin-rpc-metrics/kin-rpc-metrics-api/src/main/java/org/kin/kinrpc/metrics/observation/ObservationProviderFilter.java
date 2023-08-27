package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.TracingConfig;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.metrics.MetricsConstants;

import java.util.Objects;

/**
 * 基于{@link Observation}实现的provider端tracing filter
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@Scope(Scopes.PROVIDER)
public class ObservationProviderFilter implements Filter {
    private ObservationRegistry observationRegistry;
    private ServerObservationConvention serverObservationConvention;

    public ObservationProviderFilter() {
        ApplicationContext applicationContext = ApplicationContext.instance();
        TracingConfig tracingConfig = applicationContext.getConfig(TracingConfig.class);
        if (Objects.nonNull(tracingConfig) &&
                tracingConfig.isEnabled()) {
            observationRegistry = applicationContext.getBean(ObservationRegistry.class);
            serverObservationConvention = applicationContext.getBean(ServerObservationConvention.class);
        }
    }

    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        if (observationRegistry == null) {
            return invoker.invoke(invocation);
        }

        ServerContext serverContext = new ServerContext(invocation);
        //create observation
        Observation observation = KinRpcObservationDocumentation.SERVER.observation(
                this.serverObservationConvention,
                DefaultServerObservationConvention.instance(),
                () -> serverContext,
                observationRegistry);

        //attach
        invocation.attach(MetricsConstants.OBSERVATION_KEY, observation.start());
        return observation.scoped(() -> invoker.invoke(invocation));
    }

    @Override
    public void onResponse(Invocation invocation, RpcResponse response) {
        Observation observation = invocation.attachment(MetricsConstants.OBSERVATION_KEY);
        if (observation == null) {
            return;
        }

        Throwable exception = response.getException();
        if (Objects.nonNull(exception)) {
            //fail
            observation.error(exception);
        }
        observation.stop();
    }
}
