package org.kin.kinrpc.rpc.invoker;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.transport.rpc.ConsumerConnection;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    //该invoker代表的连接
    protected ConsumerConnection consumerConnection;

    public ReferenceInvoker(Class<?> interfaceClass, ConsumerConnection consumerConnection) {
        super(interfaceClass);
        this.consumerConnection = consumerConnection;
    }

    public abstract void init();

    public abstract void shutdown();

    protected RPCRequest createRequest(int requestId, String methodName, Object... params) {
        RPCRequest request = new RPCRequest(requestId, super.getServiceName(), methodName, params);
        return request;
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(consumerConnection.getAddress());
    }

    public boolean isActive() {
        return consumerConnection.isActive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReferenceInvoker that = (ReferenceInvoker) o;

        return consumerConnection.equals(that.consumerConnection);
    }

    @Override
    public int hashCode() {
        return consumerConnection.hashCode();
    }
}
