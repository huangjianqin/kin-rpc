package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.framework.Closeable;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.RPCThreadPool;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.RPCRetryOutException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
abstract class ClusterInvoker<I> implements Closeable {
    protected static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

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

    public Future invokeAsync(Method method, Object... params) {
        Callable callable = () -> invoke0(method, params);
        return RPCThreadPool.THREADS.submit(callable);
    }

    /**
     * 专供javassist使用
     */
    protected Future invokeAsync(String methodName, boolean isVoid, Object... params) {
        Callable callable = () -> invoke0(methodName, isVoid, params);
        return RPCThreadPool.THREADS.submit(callable);
    }

    public Object invoke0(Method method, Object... params) {
        String methodName = ClassUtils.getUniqueName(method);
        Class returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(method.getReturnType());
        return invoke0(methodName, isVoid, params);
    }

    /**
     * 专供javassist使用
     */
    protected Object invoke0(String methodName, boolean isVoid, Object... params) {
        if (retryTimes > 0) {
            int tryTimes = 0;

            //单次请求曾经fail的service 访问地址
            Set<HostAndPort> failureHostAndPorts = new HashSet<>();

            while (tryTimes < retryTimes) {
                ReferenceInvoker invoker = cluster.get(failureHostAndPorts);
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
                        break;
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
                        break;
                    }
                }
                else{
                    log.warn("cannot find valid invoker >>>> {}", methodName);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {

                    }
                }
            }

            //超过重试次数, 抛弃异常
            throw new RPCRetryOutException(retryTimes);
        } else {
            ReferenceInvoker invoker;
            while ((invoker = cluster.get(Collections.EMPTY_LIST)) == null) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {

                }
            }
            try {
                return invoker.invoke(methodName, isVoid, params);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
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
