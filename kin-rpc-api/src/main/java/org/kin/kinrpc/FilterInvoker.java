package org.kin.kinrpc;

import java.util.Objects;

/**
 * 执行{@link Filter#invoke(Invoker, Invocation)}的{@link Invoker}实现
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class FilterInvoker<T> extends DelegateInvoker<T> {
    /** 下一{@link Filter}实例 */
    private final Filter next;

    public FilterInvoker(Invoker<T> invoker) {
        this(null, invoker);
    }

    public FilterInvoker(Filter next, Invoker<T> invoker) {
        super(invoker);
        this.next = next;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        Invoker<T> delegate = getDelegate();
        return Objects.isNull(next) ? delegate.invoke(invocation) : next.invoke(delegate, invocation);
    }
}
