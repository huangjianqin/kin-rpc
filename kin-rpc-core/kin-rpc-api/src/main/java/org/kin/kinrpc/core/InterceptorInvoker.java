package org.kin.kinrpc.core;

import java.util.Objects;

/**
 * 基于{@link Interceptor}的{@link Invoker}实现
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class InterceptorInvoker<T> extends DelegateInvoker<T> {
    /** 下一{@link Interceptor}实例 */
    private final Interceptor next;

    public InterceptorInvoker(Invoker<T> invoker) {
        this(null, invoker);
    }

    public InterceptorInvoker(Interceptor next, Invoker<T> invoker) {
        super(invoker);
        this.next = next;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        Invoker<T> delegate = getDelegate();
        return Objects.isNull(next) ? delegate.invoke(invocation) : next.intercept(delegate, invocation);
    }
}
