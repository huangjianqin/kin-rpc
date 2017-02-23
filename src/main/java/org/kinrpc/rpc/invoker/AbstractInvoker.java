package org.kinrpc.rpc.invoker;

import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/2/11.
 */
public abstract class AbstractInvoker implements Invoker {
    protected final Class<?> interfaceClass;

    public AbstractInvoker(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public String getServiceName() {
        return interfaceClass.getName();
    }
}
