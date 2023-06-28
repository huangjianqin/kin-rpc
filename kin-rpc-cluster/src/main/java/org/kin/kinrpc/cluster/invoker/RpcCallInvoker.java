package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.concurrent.Timeout;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.RpcTimeoutException;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.ReferenceConstants;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 真正发起rpc call request的{@link Invoker}实现
 * 往往是interceptor chain最后一个invoker
 *
 * @author huangjianqin
 * @date 2023/6/26
 */
public final class RpcCallInvoker<T> implements Invoker<T> {
    /** 单例 */
    public static final Invoker<?> INSTANCE = new RpcCallInvoker<>();

    private RpcCallInvoker() {
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            throw new IllegalStateException("can not find method config. invocation=" + invocation);
        }

        //经过路由, 负载均衡等策略规则筛选后的invoker
        ReferenceInvoker<T> invoker = invocation.attachment(ReferenceConstants.SELECTED_INVOKER_KEY);
        if (Objects.isNull(invoker) || !invoker.isAvailable()) {
            throw new IllegalStateException("can not find available invoker. invocation=" + invocation);
        }

        CompletableFuture<Object> future = new CompletableFuture<>();

        int timeoutMs = methodConfig.getTimeout();
        Timeout timeout = null;
        if (timeoutMs > 0) {
            //async timeout
            timeout = ReferenceContext.TIMER.newTimeout((t) -> {
                if (future.isDone()) {
                    //服务调用已有返回结果
                    return;
                }

                future.completeExceptionally(new RpcTimeoutException(invocation, timeoutMs));
            }, timeoutMs, TimeUnit.MILLISECONDS);
        }

        RpcResult rpcResult = invoker.invoke(invocation);
        Timeout outterTimeout = timeout;
        rpcResult.onFinishAsync((r, t) -> {
            if (Objects.nonNull(outterTimeout)) {
                outterTimeout.cancel();
            }
        }, future, ReferenceContext.EXECUTOR);
        return RpcResult.success(invocation, future);
    }
}
