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
import java.util.concurrent.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ClusterInvoker implements InvocationHandler, AsyncInvoker, Closeable {
    private static final Logger log = LoggerFactory.getLogger("cluster");
    private static final ThreadManager THREADS = ThreadManager.forkJoinPoolThreadManager();

    private final Cluster cluster;
    private final int retryTimes;
    private final int retryTimeout;

    public ClusterInvoker(Cluster cluster, int retryTimes, int retryTimeout) {
        this.cluster = cluster;
        this.retryTimes = retryTimes;
        this.retryTimeout = retryTimeout;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");

        //异步方式: consumer端必须自定义一个与service端除了返回值为Future.class或者CompletableFuture.class外,
        //方法签名相同的接口
        Class returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType);
        if (Future.class.equals(returnType)) {
            return invokeAsync(ClassUtils.getUniqueName(method), args);
        } else if (CompletableFuture.class.equals(returnType)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return invoke0(ClassUtils.getUniqueName(method), isVoid, args);
                } catch (Exception e) {
                    log.error("", e);
                }

                return null;
            });
        }

        return invoke(ClassUtils.getUniqueName(method), isVoid, args);
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Exception {
        return invoke0(methodName, isVoid, params);
    }

    private Object invoke0(String methodName, boolean isVoid, Object... params) throws Exception {
        if(retryTimes > 0){
            int tryTimes = 0;
            while (tryTimes < retryTimes) {
                AbstractReferenceInvoker invoker = cluster.get();
                if (invoker != null) {
                    Future<RPCResponse> future = invoker.invokeAsync(methodName, params);
                    try {
                        RPCResponse rpcResponse = future.get(retryTimeout, TimeUnit.MILLISECONDS);
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
                    }catch (InterruptedException e) {
                        log.info("pending result interrupted >>> {}", e.getMessage());
                    } catch (ExecutionException e) {
                        log.info("pending result execute error >>> {}", e.getMessage());
                    } catch (TimeoutException e) {
                        log.info("invoke time out >>> {}", e.getMessage());
                    }
                }
            }

            //超过重试次数, 抛弃异常
            throw new RuntimeException("invoke get unvalid response more than " + retryTimes + " times");
        }
        else{
            AbstractReferenceInvoker invoker = cluster.get();
            if (invoker != null) {
                return invoker.invoke(methodName, isVoid, params);
            }
        }

        return null;
    }

    @Override
    public Future invokeAsync(String methodName, Object... params) throws Exception {
        Callable callable = () -> invoke0(methodName, false, params);
        Future future = THREADS.submit(callable);
        RPCContext.instance().setFuture(future);
        return future;
    }


    @Override
    public void close() {
        cluster.shutdown();
    }
}
