package org.kin.kinrpc;

import org.kin.kinrpc.config.AbstractInterfaceConfig;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public class DelegateInvoker<T> implements Invoker<T> {
    /** 下一{@link Invoker}实例 */
    private final Invoker<T> delegate;

    public DelegateInvoker(Invoker<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        return delegate.invoke(invocation);
    }

    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }

    @Override
    public AbstractInterfaceConfig<T, ?> config() {
        return delegate.config();
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    //getter
    public Invoker<T> getDelegate() {
        return delegate;
    }
}
