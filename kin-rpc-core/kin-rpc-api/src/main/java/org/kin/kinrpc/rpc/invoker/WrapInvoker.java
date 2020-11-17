package org.kin.kinrpc.rpc.invoker;

import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Url;

/**
 * 包装invoker的invoker
 *
 * @author huangjianqin
 * @date 2020/11/4
 */
public class WrapInvoker<T> implements Invoker<T> {
    /** 包装的invoker */
    protected final Invoker<T> wrapper;

    public WrapInvoker(Invoker<T> wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Object invoke(String methodName, Object[] params) throws Throwable {
        return wrapper.invoke(methodName, params);
    }

    @Override
    public Class<T> getInterface() {
        return wrapper.getInterface();
    }

    @Override
    public Url url() {
        return wrapper.url();
    }

    @Override
    public boolean isAvailable() {
        return wrapper.isAvailable();
    }

    @Override
    public void destroy() {
        wrapper.destroy();
    }
}
