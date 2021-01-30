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
import java.util.concurrent.Future;

/**
 * reference端没有必要使用字节码生成技术, 因为本来代码的实现就是把服务接口必要的参数传给provider, 仅此而已.
 *
 * @author huangjianqin
 * @date 2019-09-09
 */
final class JdkProxyClusterInvoker<T> extends ClusterInvoker<T> implements InvocationHandler {
    private RateLimiter rateLimiter;

    public JdkProxyClusterInvoker(Cluster<T> cluster, Url url, List<Notifier<?>> notifiers) {
        super(cluster, url, notifiers);
        int rate = url.getIntParam(Constants.RATE_KEY);
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

        boolean returnFuture = false;
        Class<?> returnType = method.getReturnType();
        if (Future.class.equals(returnType) || CompletableFuture.class.equals(returnType)) {
            //支持服务接口返回值是future, 直接返回内部使用的future即可
            //不支持自定义future, 只有上帝知道你的future是如何定义的
            returnFuture = true;
        }

        CompletableFuture<?> future = invokeAsync(method, returnType, args);
        if (isAsync()) {
            //async rpc call
            RpcCallContext.updateFuture(future);
            if (returnFuture) {
                return future;
            } else {
                //返回默认空值
                return ClassUtils.getDefaultValue(returnType);
            }
        } else {
            if (returnFuture) {
                return future;
            } else {
                //sync rpc call, sync 等待异步调用返回
                try {
                    return future.get();
                } catch (Exception e) {
                    throw e;
                }
            }
        }
    }
}
