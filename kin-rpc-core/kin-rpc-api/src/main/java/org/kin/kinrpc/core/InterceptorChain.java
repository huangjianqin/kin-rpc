package org.kin.kinrpc.core;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.config.AbstractInterfaceConfig;

import java.util.Comparator;
import java.util.List;

/**
 * 拦截器调用链
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class InterceptorChain<T> extends DelegateInvoker<T> {
    /** 拦截器列表, 优先级由低到高 */
    private List<Interceptor> interceptors;

    protected InterceptorChain(Invoker<T> lastInvoker, List<Interceptor> interceptors) {
        super(buildInvokerChain(lastInvoker, interceptors));
        this.interceptors = interceptors;
    }

    /**
     * 构建拦截器调用链
     *
     * @return 调用链invoker实例
     */
    private static <T> Invoker<T> buildInvokerChain(Invoker<T> lastInvoker, List<Interceptor> interceptors) {
        InterceptorInvoker<T> invokerChain = new InterceptorInvoker<>(lastInvoker);
        if (CollectionUtils.isNonEmpty(interceptors)) {
            //自定义拦截器
            interceptors.sort(Comparator.comparingInt(Interceptor::order));
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                invokerChain = new InterceptorInvoker<>(interceptors.get(i), invokerChain);
            }
        }

        return invokerChain;
    }

    public static <T> InterceptorChain<T> create(AbstractInterfaceConfig<T, ?> config, Invoker<T> lastInvoker) {
        return new InterceptorChain<>(lastInvoker, config.getInterceptors());
    }
}
