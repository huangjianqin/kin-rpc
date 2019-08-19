package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.RPCContext;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.RPCRetryOutException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ClusterInvoker implements InvocationHandler, Closeable {
    private static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);
    private static final ThreadManager THREADS = ThreadManager.commonThreadManager();

    private final Cluster cluster;
    private final int retryTimes;
    private final int retryTimeout;
    private final URL url;

    public ClusterInvoker(Cluster cluster, int retryTimes, int retryTimeout, URL url) {
        this.cluster = cluster;
        this.retryTimes = retryTimes;
        this.retryTimeout = retryTimeout;
        this.url = url;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("invoke method '" + method.getName() + "'");

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

    private Future invokeAsync(Method method, Object... params){
        Callable callable = () -> invoke0(method, params);
        Future future = THREADS.submit(callable);
        RPCContext.instance().setFuture(future);
        return future;
    }

    private Object invoke0(Method method, Object... params){
        String methodName = ClassUtils.getUniqueName(method);
        Class returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType);
        if(retryTimes > 0){
            int tryTimes = 0;

            //单次请求曾经fail的service 访问地址
            Set<HostAndPort> failureHostAndPorts = new HashSet<>();

            while (tryTimes < retryTimes) {
                AbstractReferenceInvoker invoker = cluster.get(failureHostAndPorts);
                if (invoker != null) {
                    try {
                        Future<RPCResponse> future = invoker.invokeAsync(methodName, params);
                        RPCResponse rpcResponse = future.get(retryTimeout, TimeUnit.MILLISECONDS);
                        if (rpcResponse != null) {
                            switch (rpcResponse.getState()) {
                                case SUCCESS:
                                    return rpcResponse.getResult();
                                case RETRY:
                                    tryTimes++;
                                    failureHostAndPorts.add(invoker.getAddress());
                                    break;
                                case ERROR:
                                    throw new RPCCallErrorException(rpcResponse.getInfo());
                                default:
                                    throw new UnknownRPCResponseStateCodeException(rpcResponse.getState().getCode());
                            }
                        } else {
                            tryTimes++;
                            ((RPCFuture) future).doneTimeout();
                            failureHostAndPorts.add(invoker.getAddress());
                        }
                    } catch (InterruptedException e) {

                    } catch (ExecutionException e) {
                        log.error("pending result execute error >>> {}", e.getMessage());
                    } catch (TimeoutException e) {
                        tryTimes++;
                        failureHostAndPorts.add(invoker.getAddress());
                        log.warn("invoke time out >>> {}", e.getMessage());
                    } catch (RPCRetryException e) {
                        tryTimes++;
                        failureHostAndPorts.add(invoker.getAddress());
                        log.warn(e.getMessage());
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            //超过重试次数, 抛弃异常
            throw new RPCRetryOutException(retryTimes);
        }
        else{
            AbstractReferenceInvoker invoker = cluster.get(Collections.EMPTY_LIST);
            if (invoker != null) {
                try {
                    return invoker.invoke(methodName, isVoid, params);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        //抛异常, 等待外部程序处理
        throw new CannotFindInvokerException();
    }


    @Override
    public void close() {
        cluster.shutdown();
    }

    //getter
    public URL getUrl() {
        return url;
    }
}
