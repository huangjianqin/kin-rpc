package org.kin.kinrpc.rpc.invoker;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class AbstractReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    protected static final Logger log = LoggerFactory.getLogger("invoker");
    protected RPCReference rpcReference;

    public AbstractReferenceInvoker(String serviceName, RPCReference rpcReference) {
        super(serviceName);
        this.rpcReference = rpcReference;
    }

    public abstract void init();

    public abstract void shutdown();

    protected RPCRequest createRequest(int requestId, String methodName, Object... params) {
        RPCRequest request = new RPCRequest(requestId, super.getServiceName(), methodName, params);
        return request;
    }

    public HostAndPort getAddress() {
        return rpcReference.getAddress();
    }

    public boolean isActive() {
        return rpcReference.isActive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractReferenceInvoker that = (AbstractReferenceInvoker) o;

        return rpcReference.equals(that.rpcReference);
    }

    @Override
    public int hashCode() {
        return rpcReference.hashCode();
    }
}
