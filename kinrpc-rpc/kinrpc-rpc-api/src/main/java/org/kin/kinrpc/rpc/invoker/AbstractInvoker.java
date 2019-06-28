package org.kin.kinrpc.rpc.invoker;

/**
 * Created by 健勤 on 2017/2/11.
 */
abstract class AbstractInvoker implements Invoker {
    protected final String serviceName;

    protected AbstractInvoker(String serviceName) {
        this.serviceName = serviceName;
    }

    public abstract void setRate(double rate);
    public abstract double getRate();
    //getter
    public String getServiceName() {
        return serviceName;
    }
}
