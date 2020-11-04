package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.framework.Closeable;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.rpc.RpcResponse;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.RpcRetryOutException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
abstract class ClusterInvoker<I> implements Closeable {
    protected static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    private final Cluster cluster;
    private final int retryTimes;
    private final long retryTimeout;
    private final Url url;

    public ClusterInvoker(Cluster cluster, int retryTimes, long retryTimeout, Url url) {
        this.cluster = cluster;
        this.retryTimes = retryTimes;
        this.retryTimeout = retryTimeout;
        this.url = url;
    }

    public Future<?> invokeAsync(Method method, Object... params) {
        Callable<?> callable = () -> invoke0(method, params);
        return RpcThreadPool.EXECUTORS.submit(callable);
    }

    /**
     * 专供javassist使用
     */
    protected Future<?> invokeAsync(String methodName, boolean isVoid, Object... params) {
        Callable<?> callable = () -> invoke0(methodName, isVoid, params);
        return RpcThreadPool.EXECUTORS.submit(callable);
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
                        Future<RpcResponse> future = invoker.invokeAsync(methodName, params);
                        RpcResponse rpcResponse = future.get(retryTimeout, TimeUnit.MILLISECONDS);
                        if (rpcResponse != null) {
                            switch (rpcResponse.getState()) {
                                case SUCCESS:
                                    return rpcResponse.getResult();
                                case RETRY:
                                    tryTimes++;
                                    failureHostAndPorts.add(invoker.getAddress());
                                    break;
                                case ERROR:
                                    throw new RpcCallErrorException(rpcResponse.getInfo());
                                default:
                                    throw new UnknownRpcResponseStateCodeException(rpcResponse.getState().getCode());
                            }
                        } else {
                            throw new RpcCallErrorException("rpc response call success, but get null");
                        }
                    } catch (ExecutionException e) {
                        log.error("pending result execute error >>> {}", e.getMessage());
                        break;
                    } catch (TimeoutException e) {
                        tryTimes++;
                        failureHostAndPorts.add(invoker.getAddress());
                        log.warn("invoke time out >>> {}", e.getMessage());
                    } catch (RpcRetryException e) {
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
            throw new RpcRetryOutException(retryTimes);
        } else {
            ReferenceInvoker invoker = cluster.get(Collections.emptyList());
            if (Objects.nonNull(invoker)) {
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

    public Url getUrl() {
        return url;
    }
}
