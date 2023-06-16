package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * reference端没有必要使用字节码生成技术, 因为本来代码的实现就是把服务接口必要的参数传给provider, 仅此而已.
 *
 * @author huangjianqin
 * @date 2019-09-09
 */
final class JdkProxyClusterInvoker<T> extends ClusterInvoker<T> implements InvocationHandler {
    private RateLimiter tpsLimiter;

    public JdkProxyClusterInvoker(Cluster<T> cluster, Url url, List<Notifier<?>> notifiers) {
        super(cluster, url, notifiers);
        int tps = url.getIntParam(Constants.TPS_KEY);
        if (tps > 0) {
            tpsLimiter = RateLimiter.create(tps);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("invoke method '".concat(method.getName()).concat("'"));

        //限流, 达到流量顶峰, 阻塞
        while (Objects.nonNull(tpsLimiter) && !tpsLimiter.tryAcquire()) {
            //50ms后重新尝试
            Thread.sleep(50);
        }

        Class<?> returnType = method.getReturnType();
        CompletableFuture<?> future = invokeAsync(method, returnType, args);
        return RpcCallReturnAdapters.INSTANCE.handleReturn(returnType, isAsync(), future);
    }
}
