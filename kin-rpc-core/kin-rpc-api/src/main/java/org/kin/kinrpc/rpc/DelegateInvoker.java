package org.kin.kinrpc.rpc;

import org.kin.kinrpc.rpc.common.Url;

/**
 * @author huangjianqin
 * @date 2023/2/26
 */
public class DelegateInvoker<T> implements Invoker<T>{
    private final Invoker<T> delegate;

    public DelegateInvoker(Invoker<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Result invoke(Invocation invocation) {
        return delegate.invoke(invocation);
    }

    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }

    @Override
    public Url url() {
        return delegate.url();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }
}
