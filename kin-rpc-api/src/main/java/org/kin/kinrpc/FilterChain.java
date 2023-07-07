package org.kin.kinrpc;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.config.AbstractInterfaceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected FilterChain(Invoker<T> lastInvoker, List<Filter> filters) {
        super(buildInvokerChain(lastInvoker, filters));
        this.filters = filters;
    }

    /**
     * create filter chain
     *
     * @return 调用链invoker实例
     */
    private static <T> Invoker<T> buildInvokerChain(Invoker<T> lastInvoker, List<Filter> filters) {
        FilterInvoker<T> invokerChain = new FilterInvoker<>(lastInvoker);
        if (CollectionUtils.isNonEmpty(filters)) {
            //custom filter order
            filters.sort(Comparator.comparingInt(Filter::order));
            for (int i = filters.size() - 1; i >= 0; i--) {
                invokerChain = new FilterInvoker<>(filters.get(i), invokerChain);
            }
        }

        return invokerChain;
    }

    public static <T> FilterChain<T> create(AbstractInterfaceConfig<?> config, Invoker<T> lastInvoker) {
        return new FilterChain<>(lastInvoker, config.getFilters());
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        invocation.attach(ReferenceConstants.FILTER_CHAIN, this);
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
