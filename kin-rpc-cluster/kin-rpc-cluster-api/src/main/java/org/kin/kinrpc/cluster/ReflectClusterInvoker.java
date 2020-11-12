package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2019-09-09
 * reference端没有必要使用字节码生成技术, 因为本来代码的实现就是把服务接口必要的参数传给provider, 仅此而已.
 */
final class ReflectClusterInvoker<T> extends ClusterInvoker<T> implements InvocationHandler {
    private RateLimiter rateLimiter;

    public ReflectClusterInvoker(Cluster<T> cluster, Url url) {
        super(cluster, Integer.parseInt(url.getParam(Constants.RETRY_TIMES_KEY)), Long.parseLong(url.getParam(Constants.RETRY_TIMEOUT_KEY)), url);
        int rate = Integer.parseInt(url.getParam(Constants.RATE_KEY));
        if (rate > 0) {
            rateLimiter = RateLimiter.create(rate);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        log.debug("invoke method '" + method.getName() + "'");

        //限流
        if (Objects.nonNull(rateLimiter) && !rateLimiter.tryAcquire()) {
            throw new RateLimitException(proxy.getClass().getName().concat("$").concat(method.toString()));
        }

        CompletableFuture<?> future = invokeAsync(method, args);
        if (isAsync()) {
            //async rpc call
            RpcContext.updateFuture(future);
            return null;
        } else {
            //sync rpc call
            try {
                return future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
