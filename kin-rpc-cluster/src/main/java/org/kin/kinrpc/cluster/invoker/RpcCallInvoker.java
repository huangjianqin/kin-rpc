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

        //mark reference invoker active and watch
        int invokerId = invoker.hashCode();
        int handlerId = invocation.handlerId();
        RpcStatus.watch(invokerId, handlerId);

        //ready to rpc call
        CompletableFuture<Object> future = new CompletableFuture<>();

        if (log.isDebugEnabled()) {
            log.debug("{} send rpc call. invocation={}", getClass().getSimpleName(), invocation);
        }

        //attach
        invocation.attach(InvocationConstants.RPC_CALL_START_TIME_KEY, System.currentTimeMillis());

        return invoker.invoke(invocation)
                .onFinish((r, t) -> onRpcCallResponse(invocation, r, t, future), future);
    }

    /**
     * rpc call response处理
     *
     * @param invocation    rpc call信息
     * @param result        rpc call result
     * @param t             rpc call exception
     * @param rpcCallFuture rpc call listen future
     */
    private void onRpcCallResponse(Invocation invocation,
                                   @Nullable Object result,
                                   @Nullable Throwable t,
                                   CompletableFuture<Object> rpcCallFuture) {
        if (log.isDebugEnabled()) {
            log.debug("rpc call response. result={}, exception={}, invocation={}", result, t, invocation);
        }

        invocation.attach(InvocationConstants.RPC_CALL_END_TIME_KEY, System.currentTimeMillis());

        RpcResponse rpcResponse = new RpcResponse(result, t);
        FilterChain<Object> chain = invocation.attachment(InvocationConstants.FILTER_CHAIN_KEY);
        if (Objects.nonNull(chain)) {
            chain.onResponse(invocation, rpcResponse);
        }

        afterFilterOnResponse(invocation, rpcResponse);

        if (rpcCallFuture.isDone()) {
            return;
        }

        //overwrite
        result = rpcResponse.getResult();
        t = rpcResponse.getException();
        if (Objects.isNull(t)) {
            rpcCallFuture.complete(result);
        } else {
            rpcCallFuture.completeExceptionally(t);
        }
    }

    /**
     * call after {@link Filter#onResponse(Invocation, RpcResponse)}
     *
     * @param invocation  rpc call信息
     * @param rpcResponse rpc response
     */
    private void afterFilterOnResponse(Invocation invocation,
                                       RpcResponse rpcResponse) {
        ReferenceInvoker<T> invoker = invocation.attachment(InvocationConstants.SELECTED_INVOKER_KEY);
        if (Objects.isNull(invoker)) {
            return;
        }

        int invokerId = invoker.hashCode();
        int handlerId = invocation.handlerId();

        if (!invocation.hasAttachment(InvocationConstants.RPC_CALL_START_TIME_KEY)) {
            return;
        }

        long startTime = invocation.longAttachment(InvocationConstants.RPC_CALL_START_TIME_KEY);
        long endTime = invocation.longAttachment(InvocationConstants.RPC_CALL_END_TIME_KEY, System.currentTimeMillis());
        //remoting发起rpc request耗时
        long elapsed = endTime - startTime;

        //默认true
        boolean succeeded = true;
        Throwable exception = rpcResponse.getException();
        if (Objects.nonNull(exception) &&
                !(exception instanceof ServerErrorException) &&
                !(exception instanceof RpcExceptionBlockException)) {
            //过滤服务方法执行异常 or 限流流控异常
            succeeded = false;
        }

        RpcStatus.end(invokerId, handlerId, elapsed, succeeded);
    }
}
