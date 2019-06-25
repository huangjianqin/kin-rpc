package org.kin.kinrpc.rpc.cluster;

import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.domain.RPCContext;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ClusterInvoker implements InvocationHandler, AsyncInvoker, Closeable {
    private static final Logger log = LoggerFactory.getLogger("cluster");
    private Cluster cluster;
    private static final ThreadManager THREADS = ThreadManager.forkJoinPoolThreadManager();

    public ClusterInvoker(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");

        //异步方式: consumer端必须自定义一个与service端除了返回值为Future.class或者CompletableFuture.class外,
        //方法签名相同的接口
        Class returnType = method.getReturnType();
        if (Future.class.equals(returnType)) {
            return invokeAsync(ClassUtils.getUniqueName(method), args);
        } else if (CompletableFuture.class.equals(returnType)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return invoke0(ClassUtils.getUniqueName(method), args);
                } catch (Exception e) {
                    ExceptionUtils.log(e);
                }

                return null;
            });
        }

        return invoke(ClassUtils.getUniqueName(method), Void.class.equals(returnType), args);
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Exception {
        return invoke0(methodName, params);
    }

    private Object invoke0(String methodName, Object... params) throws Exception {
        try {
            int tryTimes = 0;
            while (tryTimes < ClusterConstants.RETRY_TIMES) {
                AbstractReferenceInvoker invoker = cluster.get();
                if (invoker != null) {
                    Future<RPCResponse> future = invoker.invokeAsync(methodName, params);
                    RPCResponse rpcResponse = future.get(200, TimeUnit.MILLISECONDS);
                    if (rpcResponse != null) {
                        switch (rpcResponse.getState()) {
                            case SUCCESS:
                                return rpcResponse.getResult();
                            case RETRY:
                                tryTimes++;
                                break;
                            case ERROR:
                                throw new RuntimeException(rpcResponse.getInfo());
                            default:
                                throw new RuntimeException("unknown rpc response state code");
                        }
                    } else {
                        tryTimes++;
                        ((RPCFuture) future).doneTimeout();
                    }
                }
            }
        } catch (Throwable throwable) {
            ExceptionUtils.log(throwable);
        }

        //超过重试次数, 抛弃异常
        throw new RuntimeException("invoke get unvalid response more than " + ClusterConstants.RETRY_TIMES + " times");
    }

    @Override
    public Future invokeAsync(String methodName, Object... params) throws Exception {
        Callable callable = () -> invoke0(methodName, params);
        Future future = THREADS.submit(callable);
        RPCContext.instance().setFuture(future);
        return future;
    }


    @Override
    public void close() {
        cluster.shutdown();
    }
}
