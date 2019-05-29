package org.kin.kinrpc.rpc.invoker;

import org.apache.log4j.Logger;
import org.kin.kinrpc.remoting.transport.bootstrap.ReferenceConnection;
import org.kin.kinrpc.rpc.protocol.RPCRequest;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    private static final Logger log = Logger.getLogger(ReferenceInvoker.class);

    //该invoker代表的连接
    protected ReferenceConnection connection;
    //远程服务端的一些信息

    public ReferenceInvoker(Class<?> interfaceClass, ReferenceConnection connection) {
        super(interfaceClass);
        this.connection = connection;
    }

    public abstract void init();
    public abstract void shutdown();

    public RPCRequest createRequest(int requestId, String methodName, Object... params){
        RPCRequest request = new RPCRequest(requestId, super.getServiceName(), methodName, params);
        return request;
    }

    public String getAddress(){
        return connection.getAddress();
    }
}
