package org.kin.kinrpc.metrics.observation;

import io.micrometer.common.KeyValues;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.constants.InvocationConstants;

import java.util.Objects;

import static org.kin.kinrpc.metrics.observation.KinRpcObservationDocumentation.LowCardinalityKeyNames.NET_PEER_NAME;
import static org.kin.kinrpc.metrics.observation.KinRpcObservationDocumentation.LowCardinalityKeyNames.NET_PEER_PORT;

/**
 * default client observation convention
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public class DefaultClientObservationConvention extends AbstractObservationConvention implements ClientObservationConvention {
    /** 单例 */
    private static final DefaultClientObservationConvention INSTANCE = new DefaultClientObservationConvention();

    public static DefaultClientObservationConvention instance() {
        return INSTANCE;
    }

    private DefaultClientObservationConvention() {
    }

    @Override
    public String getName() {
        return "rpc.client.duration";
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ClientContext context) {
        KeyValues keyValues = super.getLowCardinalityKeyValues(context.getInvocation());
        //带上remote invoker address tag
        return withRemoteHostPort(keyValues, context);
    }

    /**
     * 写入remote invoker address tag
     */
    private KeyValues withRemoteHostPort(KeyValues keyValues, ClientContext context) {
        //发起rpc call invoker
        ReferenceInvoker<?> invoker = context.getInvocation().attachment(InvocationConstants.RPC_CALL_INVOKER_KEY);
        if (Objects.isNull(invoker)) {
            return keyValues;
        }

        //invoker instance
        ServiceInstance serviceInstance = invoker.serviceInstance();
        return withRemoteHostPort(keyValues, serviceInstance.host(), serviceInstance.port());
    }

    /**
     * 写入remote invoker address tag
     */
    private KeyValues withRemoteHostPort(KeyValues keyValues, String remoteHostName, int remotePort) {
        keyValues = appendNonNull(keyValues, NET_PEER_NAME, remoteHostName);
        if (remotePort == 0) {
            return keyValues;
        }
        return appendNonNull(keyValues, NET_PEER_PORT, String.valueOf(remotePort));
    }

    @Override
    public String getContextualName(ClientContext context) {
        return super.getContextualName(context.getInvocation());
    }
}