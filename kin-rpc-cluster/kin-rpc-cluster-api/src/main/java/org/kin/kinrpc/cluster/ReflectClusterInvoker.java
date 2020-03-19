package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.exception.RateLimitException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2019-09-09
 * reference端没有必要使用字节码生成技术, 因为本来代码的实现就是把服务接口必要的参数传给provider, 仅此而已.
 */
class ReflectClusterInvoker extends ClusterInvoker implements InvocationHandler {
    private RateLimiter rateLimiter;

    public ReflectClusterInvoker(Cluster cluster, int retryTimes, int retryTimeout, URL url) {
        super(cluster, retryTimes, retryTimeout, url);
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

        //异步方式: reference端必须自定义一个与service端除了返回值为Future.class或者CompletableFuture.class外,
        //方法签名相同的接口
        Class returnType = method.getReturnType();
        if (Future.class.equals(returnType)) {
            return invokeAsync(method, args);
        } else if (CompletableFuture.class.equals(returnType)) {
            return CompletableFuture.supplyAsync(() -> invoke0(method, args));
        }

        return invoke0(method, args);
    }
}
