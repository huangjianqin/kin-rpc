package org.kin.kinrpc;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.constants.InvocationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * filter invoker chain
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class FilterChain<T> extends DelegateInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FilterChain.class);

    /** filter list, 优先级由低到高 */
    private final List<Filter> filters;

    protected FilterChain(Invoker<T> tailInvoker, List<Filter> filters) {
        super(buildInvokerChain(tailInvoker, filters));
        this.filters = filters;
    }

    /**
     * create filter chain
     *
     * @return 调用链invoker实例
     */
    private static <T> Invoker<T> buildInvokerChain(Invoker<T> rpcCallInvoker, List<Filter> filters) {
        FilterInvoker<T> invokerChain = new FilterInvoker<>(rpcCallInvoker);
        if (CollectionUtils.isNonEmpty(filters)) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                invokerChain = new FilterInvoker<>(filters.get(i), invokerChain);
            }
        }

        return invokerChain;
    }

    /**
     * 创建{@link FilterChain}实例
     *
     * @param userFilters         user defined filter
     * @param internalPostFilters 内部后置filter, 不参与user defined filter order sort
     * @param tailInvoker         tail invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> userFilters,
                                            List<Filter> internalPostFilters,
                                            Invoker<T> tailInvoker) {
        return create(Collections.emptyList(), userFilters, internalPostFilters, tailInvoker);
    }

    /**
     * 创建{@link FilterChain}实例
     *
     * @param userFilters user defined filter
     * @param tailInvoker tail invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> userFilters,
                                            Invoker<T> tailInvoker) {
        return create(Collections.emptyList(), userFilters, Collections.emptyList(), tailInvoker);
    }

    /**
     * 创建{@link FilterChain}实例
     *
     * @param internalPreFilters  内部前置filter, 不参与user defined filter order sort
     * @param userFilters         user defined filter
     * @param internalPostFilters 内部后置filter, 不参与user defined filter order sort
     * @param tailInvoker         tail invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> internalPreFilters,
                                            List<Filter> userFilters,
                                            List<Filter> internalPostFilters,
                                            Invoker<T> tailInvoker) {
        //sort user defined filter
        userFilters.sort(Comparator.comparingInt(Filter::order));

        List<Filter> finalFilters = new ArrayList<>();
        finalFilters.addAll(internalPreFilters);
        finalFilters.addAll(userFilters);
        finalFilters.addAll(internalPostFilters);

        // TODO: 2023/7/10 加载内置filter, 某些功能支持需要通过filter实现, 但我们不需要user手动配置, 同时filter还需要init
        return new FilterChain<>(tailInvoker, finalFilters);
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        CompletableFuture<Object> invokeFuture = new CompletableFuture<>();
        super.invoke(invocation)
                .onFinish((r, t) -> onInvokeFinish(invocation, r, t, invokeFuture));
        return RpcResult.success(invocation, invokeFuture);
    }

    /**
     * call after filter chain invoke finish
     *
     * @param invocation   rpc call信息
     * @param result       rpc call result
     * @param t            rpc call exception
     * @param invokeFuture filter chain invoke listen future
     */
    private void onInvokeFinish(Invocation invocation,
                                @Nullable Object result,
                                @Nullable Throwable t,
                                CompletableFuture<Object> invokeFuture) {
        invocation.attach(InvocationConstants.RPC_CALL_END_TIME_KEY, System.currentTimeMillis());

        //call Filter#onResponse
        RpcResponse rpcResponse = new RpcResponse(result, t);
        onResponse(invocation, rpcResponse);

        //overwrite result or exception
        result = rpcResponse.getResult();
        t = rpcResponse.getException();
        if (Objects.isNull(t)) {
            invokeFuture.complete(result);
        } else {
            invokeFuture.completeExceptionally(t);
        }
    }

    /**
     * rpc response时触发
     *
     * @param invocation rpc call信息
     * @param response   rpc response
     */
    private void onResponse(Invocation invocation,
                            RpcResponse response) {
        for (Filter filter : filters) {
            try {
                filter.onResponse(invocation, response);
            } catch (Exception e) {
                log.error("{}#onResponse error, invocation={}, response={}",
                        filter.getClass().getName(), invocation, response, e);
            }
        }
    }
}
