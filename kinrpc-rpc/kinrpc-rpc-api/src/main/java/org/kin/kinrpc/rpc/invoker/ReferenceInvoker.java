package org.kin.kinrpc.rpc.invoker;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    //该invoker代表的连接
    protected RPCReference rpcReference;

    public ReferenceInvoker(Class<?> interfaceClass, RPCReference rpcReference) {
        super(interfaceClass);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReferenceInvoker that = (ReferenceInvoker) o;

        return rpcReference.equals(that.rpcReference);
    }

    @Override
    public int hashCode() {
        return rpcReference.hashCode();
    }
}
