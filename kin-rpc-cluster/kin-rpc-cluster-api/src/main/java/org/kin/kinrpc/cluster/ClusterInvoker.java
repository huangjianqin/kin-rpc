package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.framework.Closeable;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.RpcResponse;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.RpcRetryOutException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by 健勤 on 2017/2/15.
 */
abstract class ClusterInvoker<T> implements Closeable {
    protected static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    private final Cluster<T> cluster;
    private final int retryTimes;
    private final long retryTimeout;
    private final Url url;
    /** async rpc call 事件通知 */
    private final Map<Class<?>, Notifier<?>> returnType2Notifier;

    public ClusterInvoker(Cluster<T> cluster, Url url, List<Notifier<?>> notifiers) {
        this.cluster = cluster;
        this.url = url;
        this.retryTimes = Integer.parseInt(url.getParam(Constants.RETRY_TIMES_KEY));
        this.retryTimeout = Long.parseLong(url.getParam(Constants.RETRY_TIMEOUT_KEY));

        Map<Class<?>, Notifier<?>> returnType2Notifier = new HashMap<>();

        for (Notifier<?> notifier : notifiers) {
            List<Class<?>> returnTypes = ClassUtils.getSuperInterfacesGenericActualTypes(notifier.getClass());
            returnType2Notifier.put(returnTypes.get(0), notifier);
        }

        this.returnType2Notifier = Collections.unmodifiableMap(returnType2Notifier);
    }

    /**
     * async rpc call
     */
    public CompletableFuture<?> invokeAsync(Method method, Class<?> returnType, Object... params) {
        return invokeAsync(method.getName(), returnType, params);
    }

    /**
     * async rpc call
     */
    public CompletableFuture<?> invokeAsync(String methodName, Class<?> returnType, Object... params) {
        CompletableFuture<Object> rpcCallFuture = CompletableFuture.supplyAsync(() -> invoke0(methodName, params), RpcThreadPool.EXECUTORS);
        if (isAsync()) {
            //触发notifier
            rpcCallFuture.thenAcceptAsync(obj -> {
                Notifier notifier = getNotifier(returnType);
                if (Objects.nonNull(notifier)) {
                    Class<?> rpcCallResultType = obj.getClass();
                    if (Throwable.class.isAssignableFrom(rpcCallResultType)) {
                        //异常
                        notifier.handlerException((Throwable) obj);
                    } else {
                        notifier.onRpcCallSuc(obj);
                    }
                }
            }, RpcThreadPool.EXECUTORS);
        }
        return rpcCallFuture;
    }

    /**
     * rpc call
     */
    protected Object invoke0(String methodName, Object... params) {
        if (retryTimes > 0) {
            int tryTimes = 0;

            //单次请求曾经fail的service 访问地址
            Set<HostAndPort> failureHostAndPorts = new HashSet<>();

            while (tryTimes < retryTimes) {
                AsyncInvoker<T> invoker = cluster.get(failureHostAndPorts);
                if (invoker != null) {
                    HostAndPort address = HostAndPort.fromString(invoker.url().getAddress());
                    try {
                        Future<RpcResponse> future = invoker.invokeAsync(methodName, params);
                        RpcResponse rpcResponse = future.get(retryTimeout, TimeUnit.MILLISECONDS);
                        if (rpcResponse != null) {
                            switch (rpcResponse.getState()) {
                                case SUCCESS:
                                    return rpcResponse.getResult();
                                case RETRY:
                                    tryTimes++;
                                    failureHostAndPorts.add(address);
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
                        failureHostAndPorts.add(address);
                        log.warn("invoke time out >>> {}", e.getMessage());
                    } catch (RpcRetryException e) {
                        tryTimes++;
                        failureHostAndPorts.add(address);
                        log.warn(e.getMessage());
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                        break;
                    }
                } else {
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
            AsyncInvoker<T> invoker = cluster.get(Collections.emptyList());
            if (Objects.nonNull(invoker)) {
                try {
                    return invoker.invoke(methodName, params);
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

    /**
     * @return rpc call是否异步
     */
    protected boolean isAsync() {
        return Boolean.parseBoolean(url.getParam(Constants.ASYNC_KEY));
    }

    /**
     * 根据返回类型获取notifier
     */
    protected Notifier<?> getNotifier(Class<?> returnType) {
        return returnType2Notifier.get(returnType);
    }

    //getter
    public Url getUrl() {
        return url;
    }
}
