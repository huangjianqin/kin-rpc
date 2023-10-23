package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.TracingConfig;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.metrics.MetricsConstants;

import java.util.Objects;

/**
 * 基于{@link Observation}实现的consumer端tracing filter
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@Scope(Scopes.CONSUMER)
public class ObservationConsumerFilter implements Filter {
    private ObservationRegistry observationRegistry;
    private ClientObservationConvention clientObservationConvention;

    public ObservationConsumerFilter() {
        ApplicationContext applicationContext = ApplicationContext.instance();
        TracingConfig tracingConfig = applicationContext.getConfig(TracingConfig.class);
        if (Objects.nonNull(tracingConfig) &&
                tracingConfig.isEnabled()) {
            observationRegistry = applicationContext.getBean(ObservationRegistry.class);
            clientObservationConvention = applicationContext.getBean(ClientObservationConvention.class);
        }
    }

    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        if (observationRegistry == null) {
            return invoker.invoke(invocation);
        }

        ClientContext clientContext = new ClientContext(invocation);
        //create observation
        Observation observation = KinRpcObservationDocumentation.CLIENT.observation(
                this.clientObservationConvention,
                DefaultClientObservationConvention.instance(),
                () -> clientContext,
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
