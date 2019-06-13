package org.kin.kinrpc.rpc.invoker;

import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.cluster.Cluster;
import org.kin.kinrpc.rpc.cluster.ClusterConstants;
import org.kin.kinrpc.rpc.utils.ClassUtils;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
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
public class ClusterInvoker implements InvocationHandler, AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger("cluster");
    private static final ThreadManager threads = ThreadManager.forkJoinPoolThreadManager();

    private Cluster cluster;

    public ClusterInvoker(Cluster cluster) {
        this.cluster = cluster;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");

        //异步方式: consumer端必须自定义一个与service端除了返回值为Future.class或者CompletableFuture.class外,
        //方法签名相同的接口
        Class returnType = method.getReturnType();
        if(Future.class.equals(returnType)){
            return invokerAsync(ClassUtils.getUniqueName(method), args);
        }
        else if(CompletableFuture.class.equals(returnType)){
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return getAsyncInvocationDetail(ClassUtils.getUniqueName(method), args).call();
                } catch (Exception e) {
                    ExceptionUtils.log(e);
                }

                return null;
            });
        }

        return invoke(ClassUtils.getUniqueName(method), Void.class.equals(returnType), args);
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        int tryTimes = 0;
        while (tryTimes < ClusterConstants.RETRY_TIMES) {
            ReferenceInvoker invoker = cluster.get();
            if (invoker != null) {
                RPCResponse rpcResponse = (RPCResponse) invoker.invoke(methodName, isVoid, params);
                if(rpcResponse != null){
                    switch (rpcResponse.getState()) {
                        case SUCCESS:
                            return rpcResponse.getResult();
                        case RETRY:
                            tryTimes++;
                            break;
                        case ERROR:
                            throw new RuntimeException(rpcResponse.getInfo());
                    }
                }
                else{
                    tryTimes++;
                }
            }
        }
        //超过重试次数, 抛弃异常
        throw new RuntimeException("invoke get unvalid response more than " + ClusterConstants.RETRY_TIMES + " times");
    }

    @Override
    public Future invokerAsync(String methodName, Object... params) throws Throwable {
        return threads.submit(getAsyncInvocationDetail(methodName, params));
    }

    private Callable getAsyncInvocationDetail(String methodName, Object... params){
        return () -> {
            try {
                int tryTimes = 0;
                while (tryTimes < ClusterConstants.RETRY_TIMES) {
                    ReferenceInvoker invoker = cluster.get();
                    if (invoker != null) {
                        Future<RPCResponse> future = invoker.invokerAsync(methodName, params);
                        RPCResponse rpcResponse = future.get(200, TimeUnit.MILLISECONDS);
                        if(rpcResponse != null){
                            switch (rpcResponse.getState()) {
                                case SUCCESS:
                                    return rpcResponse.getResult();
                                case RETRY:
                                    tryTimes++;
                                    break;
                                case ERROR:
                                    throw new RuntimeException(rpcResponse.getInfo());
                            }
                        }
                        else{
                            tryTimes++;
                        }
                    }
                }
            } catch (Throwable throwable) {
                ExceptionUtils.log(throwable);
            }

            //超过重试次数, 抛弃异常
            throw new RuntimeException("invoke get unvalid response more than " + ClusterConstants.RETRY_TIMES + " times");
        };
    }
}
