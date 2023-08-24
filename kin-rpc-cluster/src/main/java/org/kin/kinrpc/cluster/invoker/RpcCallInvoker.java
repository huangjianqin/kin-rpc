package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.constants.InvocationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Objects;

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
        ReferenceInvoker<T> invoker = invocation.attachment(InvocationConstants.RPC_CALL_INVOKER_KEY);
        if (Objects.isNull(invoker) || !invoker.isAvailable()) {
            return RpcResult.fail(invocation, new IllegalStateException("can not find available invoker. invocation=" + invocation));
        }

        if (log.isDebugEnabled()) {
            log.debug("{} send rpc call. invocation={}", getClass().getSimpleName(), invocation);
        }

        return invoker.invoke(invocation)
                .onFinish((r, t) -> onRpcCallResponse(invocation, r, t));
    }

    /**
     * rpc call response处理
     *
     * @param invocation rpc call信息
     * @param result     rpc call result
     * @param t          rpc call exception
     */
    private void onRpcCallResponse(Invocation invocation,
                                   @Nullable Object result,
                                   @Nullable Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug("rpc call response. result={}, exception={}, invocation={}", result, t, invocation);
        }
    }
}
