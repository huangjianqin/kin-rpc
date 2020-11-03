package org.kin.kinrpc.rpc.invoker;

/**
 * Created by 健勤 on 2017/2/11.
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {
    protected final String serviceName;

    protected AbstractInvoker(String serviceName) {
        this.serviceName = serviceName;
    }

    //getter

    public String getServiceName() {
        return serviceName;
    }
}
