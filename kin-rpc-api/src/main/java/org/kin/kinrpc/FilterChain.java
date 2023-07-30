package org.kin.kinrpc;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.constants.InvocationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    protected FilterChain(Invoker<T> rpcCallInvoker, List<Filter> filters) {
        super(buildInvokerChain(rpcCallInvoker, filters));
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
     * @param rpcCallInvoker      发起rpc call invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> userFilters,
                                            List<Filter> internalPostFilters,
                                            Invoker<T> rpcCallInvoker) {
        return create(Collections.emptyList(), userFilters, internalPostFilters, rpcCallInvoker);
    }

    /**
     * 创建{@link FilterChain}实例
     *
     * @param userFilters user defined filter
     * @param rpcCallInvoker 发起rpc call invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> userFilters,
                                            Invoker<T> rpcCallInvoker) {
        return create(Collections.emptyList(), userFilters, Collections.emptyList(), rpcCallInvoker);
    }

    /**
     * 创建{@link FilterChain}实例
     *
     * @param internalPreFilters  内部前置filter, 不参与user defined filter order sort
     * @param userFilters         user defined filter
     * @param internalPostFilters 内部后置filter, 不参与user defined filter order sort
     * @param rpcCallInvoker         发起rpc call invoker
     * @return {@link FilterChain}实例
     */
    public static <T> FilterChain<T> create(List<Filter> internalPreFilters,
                                            List<Filter> userFilters,
                                            List<Filter> internalPostFilters,
                                            Invoker<T> rpcCallInvoker) {
        //sort user defined filter
        userFilters.sort(Comparator.comparingInt(Filter::order));

        List<Filter> finalFilters = new ArrayList<>();
        finalFilters.addAll(internalPreFilters);
        finalFilters.addAll(userFilters);
        finalFilters.addAll(internalPostFilters);

        // TODO: 2023/7/10 加载内置filter, 某些功能支持需要通过filter实现, 但我们不需要user手动配置, 同时filter还需要init
        return new FilterChain<>(rpcCallInvoker, finalFilters);
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        invocation.attach(InvocationConstants.FILTER_CHAIN_KEY, this);
        return super.invoke(invocation);
    }

    /**
     * rpc response时触发
     *
     * @param invocation rpc call信息
     * @param response   rpc response
     */
    public void onResponse(Invocation invocation,
                           RpcResponse response) {
        for (Filter filter : filters) {
            try {
                filter.onResponse(invocation, response);
            } catch (Exception e) {
                log.error("{}#onResponse error", filter.getClass().getName(), e);
            }
        }
    }
}
