package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.InvocationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 真正发起rpc call request的{@link Invoker}实现
 * 往往是filter chain最后一个invoker
 *
 * @author huangjianqin
 * @date 2023/6/26
 */
public final class RpcCallInvoker<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(RpcCallInvoker.class);

    /** 单例 */
    private static final RpcCallInvoker<?> INSTANCE = new RpcCallInvoker<>();

    public static RpcCallInvoker<?> instance() {
        return INSTANCE;
    }

    private RpcCallInvoker() {
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        //经过路由, 负载均衡等策略规则筛选后的invoker
        ReferenceInvoker<T> invoker = invocation.attachment(InvocationConstants.SELECTED_INVOKER_KEY);
        if (Objects.isNull(invoker) || !invoker.isAvailable()) {
            return RpcResult.fail(invocation, new IllegalStateException("can not find available invoker. invocation=" + invocation));
        }

        //ready to rpc call
        long now = System.currentTimeMillis();
        invocation.attach(InvocationConstants.RPC_CALL_START_TIME_KEY, now);

        CompletableFuture<Object> future = new CompletableFuture<>();

        if (log.isDebugEnabled()) {
            log.debug("{} send rpc call. invocation={}", getClass().getSimpleName(), invocation);
        }

        return invoker.invoke(invocation)
                .onFinish((r, t) -> onRpcCallResponse(invocation, r, t, future), future);
    }

    /**
     * rpc call response处理
     *
     * @param invocation    rpc call信息
     * @param result        rpc call result
     * @param t             rpc call exception
     * @param future        listen rpc call future
     */
    private void onRpcCallResponse(Invocation invocation,
                                   @Nullable Object result,
                                   @Nullable Throwable t,
                                   CompletableFuture<Object> future) {
        if (future.isDone()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("rpc call response. result={}, exception={}, invocation={}", result, t, invocation);
        }

        invocation.attach(InvocationConstants.RPC_CALL_FINISH_TIME_KEY, System.currentTimeMillis());

        RpcResponse rpcResponse = new RpcResponse(result, t);
        FilterChain<T> chain = invocation.attachment(InvocationConstants.FILTER_CHAIN);
        if (Objects.nonNull(chain)) {
            chain.onResponse(invocation, rpcResponse);
        }

        //overwrite
        result = rpcResponse.getResult();
        t = rpcResponse.getException();
        if (Objects.isNull(t)) {
            future.complete(result);
        } else {
            future.completeExceptionally(t);
        }
    }
}
