package org.kin.kinrpc.metrics.observation;

import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;
import org.kin.kinrpc.Invocation;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ClientContext extends SenderContext<Invocation> {
    /** rpc call信息 */
    private final Invocation invocation;

    public ClientContext(Invocation invocation) {
        super((map, key, value) -> Objects.requireNonNull(map).attach(key, value), Kind.CLIENT);
        this.invocation = invocation;
        setCarrier(invocation);
    }

    //getter
    public Invocation getInvocation() {
        return invocation;
    }
}
