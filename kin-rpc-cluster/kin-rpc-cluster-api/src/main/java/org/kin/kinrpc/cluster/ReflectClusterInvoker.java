package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2019-09-09
 * reference端没有必要使用字节码生成技术, 因为本来代码的实现就是把服务接口必要的参数传给provider, 仅此而已.
 */
final class ReflectClusterInvoker<T> extends ClusterInvoker<T> implements InvocationHandler {
    private RateLimiter rateLimiter;

    public ReflectClusterInvoker(Cluster<T> cluster, Url url, List<Notifier<?>> notifiers) {
        super(cluster, url, notifiers);
        int rate = Integer.parseInt(url.getParam(Constants.RATE_KEY));
        if (rate > 0) {
            rateLimiter = RateLimiter.create(rate);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("invoke method '".concat(ClassUtils.getUniqueName(method)).concat("'"));

        //限流
        if (Objects.nonNull(rateLimiter) && !rateLimiter.tryAcquire()) {
            throw new RateLimitException(proxy.getClass().getName().concat("$").concat(method.toString()));
        }

        CompletableFuture<?> future = invokeAsync(method, method.getReturnType(), args);
        if (isAsync()) {
            //async rpc call
            RpcContext.updateFuture(future);
            return null;
        } else {
            //sync rpc call
            try {
                return future.get();
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
