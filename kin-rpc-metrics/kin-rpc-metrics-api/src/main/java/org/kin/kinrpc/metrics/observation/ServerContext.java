package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.ReceiverContext;
import org.kin.kinrpc.Invocation;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ServerContext extends ReceiverContext<Invocation> {
    /** rpc call信息 */
    private final Invocation invocation;

    public ServerContext(Invocation invocation) {
        super((stringObjectMap, s) -> stringObjectMap.attachment(s, ""), Kind.SERVER);
        this.invocation = invocation;
        setCarrier(invocation);
    }

    //getter
    public Invocation getInvocation() {
        return invocation;
    }
}
