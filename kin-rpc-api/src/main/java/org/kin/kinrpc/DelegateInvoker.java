package org.kin.kinrpc;

import java.util.Objects;

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

    //getter
    public Invoker<T> getDelegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DelegateInvoker)) {
            return false;
        }
        DelegateInvoker<?> that = (DelegateInvoker<?>) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
