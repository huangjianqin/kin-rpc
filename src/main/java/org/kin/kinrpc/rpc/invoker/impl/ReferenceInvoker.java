package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.kinrpc.rpc.invoker.AbstractInvoker;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.kin.kinrpc.transport.rpc.ConsumerConnection;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger(ReferenceInvoker.class);

    //该invoker代表的连接
    protected ConsumerConnection connection;
    //远程服务端的一些信息

    public ReferenceInvoker(Class<?> interfaceClass, ConsumerConnection connection) {
        super(interfaceClass);
        this.connection = connection;
    }

    public abstract void init();

    public abstract void shutdown();

    public RPCRequest createRequest(int requestId, String methodName, Object... params) {
        RPCRequest request = new RPCRequest(requestId, super.getServiceName(), methodName, params);
        return request;
    }

    public String getAddress() {
        return connection.getAddress();
    }
}
