package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.concurrent.Timeout;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.RpcTimeoutException;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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
    private static final Logger log = LoggerFactory.getLogger(RpcCallInvoker.class);

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

        //ready to rpc call
        long now = System.currentTimeMillis();
        invocation.attach(ReferenceConstants.RPC_CALL_START_TIME_KEY, now);

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
            invocation.attach(ReferenceConstants.TIMEOUT_KEY, now + timeoutMs);
        }

        RpcResult rpcResult = invoker.invoke(invocation);
        Timeout outterTimeout = timeout;
        rpcResult.onFinishAsync((r, t) -> onRpcCallResponse(invocation, r, t, outterTimeout, future), ReferenceContext.EXECUTOR);
        return RpcResult.success(invocation, future);
    }

    /**
     * rpc call response处理
     *
     * @param invocation rpc call信息
     * @param result     rpc call result
     * @param t          rpc call exception
     * @param future     listen rpc call future
     */
    private void onRpcCallResponse(Invocation invocation,
                                   @Nullable Object result,
                                   @Nullable Throwable t,
                                   @Nullable Timeout timeout,
                                   CompletableFuture<Object> future) {
        if (log.isDebugEnabled()) {
            log.debug("rpc call response. result={}, exception={}, invocation={}", result, t, invocation);
        }

        invocation.attach(ReferenceConstants.RPC_CALL_FINISH_TIME_KEY, System.currentTimeMillis());
        if (Objects.nonNull(timeout)) {
            timeout.cancel();
        }

        RpcResponse rpcResponse = new RpcResponse(result, t);
        InterceptorChain chain = invocation.attachment(ReferenceConstants.INTERCEPTOR_CHAIN);
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
