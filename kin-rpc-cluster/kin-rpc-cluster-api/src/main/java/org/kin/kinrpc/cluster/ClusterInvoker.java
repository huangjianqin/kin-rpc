package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.framework.Closeable;
import org.kin.framework.concurrent.Timeout;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcCallRetryException;
import org.kin.kinrpc.rpc.exception.RpcCallRetryOutException;
import org.kin.kinrpc.rpc.exception.RpcCallTimeOutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 健勤 on 2017/2/15.
 */
abstract class ClusterInvoker<T> implements Closeable {
    protected static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    private final Cluster<T> cluster;
    private final Url url;
    /** 最大重试次数 */
    private final int retryTimes;
    /** 重试间隔 */
    private final int retryInterval;
    /** rpc call 超时时间 */
    private final int callTimeout;
    /** async rpc call 事件通知 */
    private final Map<Class<?>, Notifier<?>> returnType2Notifier;

    public ClusterInvoker(Cluster<T> cluster, Url url, List<Notifier<?>> notifiers) {
        this.cluster = cluster;
        this.url = url;
        this.retryTimes = url.getIntParam(Constants.RETRY_TIMES_KEY);
        this.retryInterval = url.getIntParam(Constants.RETRY_INTERVAL_KEY);
        this.callTimeout = url.getIntParam(Constants.CALL_TIMEOUT_KEY);

        Map<Class<?>, Notifier<?>> returnType2Notifier = new HashMap<>();

        for (Notifier<?> notifier : notifiers) {
            List<Class<?>> returnTypes = ClassUtils.getSuperInterfacesGenericRawTypes(Notifier.class, notifier.getClass());
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<?> invokeAsync(String methodName, Class<?> returnType, Object... params) {
        ClusterInvocation clusterInvocation = new ClusterInvocation();
        invoke0(clusterInvocation, methodName, params);

        if (isAsync()) {
            //触发notifier
            clusterInvocation.consumer.thenAccept(obj -> {
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
            });
        }
        return clusterInvocation.consumer;
    }

    /**
     * rpc call
     */
    protected void invoke0(ClusterInvocation clusterInvocation, String methodName, Object[] params) {
        try {
            invoke1(clusterInvocation, methodName, params);
        } catch (CannotFindInvokerException e) {
            handleInvokeException(e, clusterInvocation, methodName, params);
        }
    }

    /**
     * rpc call
     */
    protected void invoke1(ClusterInvocation clusterInvocation, String methodName, Object[] params) {
        AsyncInvoker<T> invoker = cluster.get(clusterInvocation.failureHostAndPorts);
        if (invoker != null) {
            //rpc call 返回结果 provier future
            //provider需要保证CompletableFuture stage已经在RpcThreadPool.executors()上执行
            CompletableFuture<Object> invokeFuture = invoker.invokeAsync(methodName, params);
            //mark 本次rpc remote call的invoker
            clusterInvocation.rpcCall(invoker);
            //rpc invoke 后, 对结果(异常)的处理
            CompletableFuture<Void> afterInvokeFuture = invokeFuture.thenAccept(clusterInvocation::done)
                    .exceptionally(throwable -> {
                        handleInvokeException(throwable, clusterInvocation, methodName, params);
                        return null;
                    });
            if (callTimeout > 0 && !invokeFuture.isDone()) {
                Timeout rpcCallTimeOut = RpcThreadPool.wheelTimer()
                        .newTimeout(t -> invokeFuture.completeExceptionally(new RpcCallTimeOutException("rpc call time out")),
                                callTimeout, TimeUnit.MILLISECONDS);
                afterInvokeFuture.whenComplete((result, error) -> {
                    //如果正常结束rpc invoke, clean time out
                    if (!rpcCallTimeOut.isExpired() || !rpcCallTimeOut.isCancelled()) {
                        rpcCallTimeOut.cancel();
                    }
                });
            }
        } else {
            throw new CannotFindInvokerException(url.getServiceKey(), methodName);
        }
    }

    private void handleInvokeException(Throwable throwable,
                                       ClusterInvocation clusterInvocation,
                                       String methodName,
                                       Object[] params) {
        if (throwable instanceof RpcCallRetryException ||
                throwable instanceof RpcCallTimeOutException ||
                throwable instanceof CannotFindInvokerException) {
            //重试异常, 尝试retry rpc call
            if (clusterInvocation.failure()) {
                RpcThreadPool.executors().schedule(
                        () -> invoke1(clusterInvocation, methodName, params),
                        retryInterval, TimeUnit.MILLISECONDS);
            } else {
                //超过重试次数, 抛弃异常
                if (retryTimes > 0) {
                    clusterInvocation.doneError(new RpcCallRetryOutException(retryTimes));
                } else {
                    clusterInvocation.doneError(new RpcCallErrorException("", throwable));
                }
            }
        } else {
            clusterInvocation.doneError(throwable);
        }
    }

    @Override
    public void close() {
        cluster.shutdown();
    }

    /**
     * @return rpc call是否异步
     */
    protected boolean isAsync() {
        return url.getBooleanParam(Constants.ASYNC_KEY);
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

    //-------------------------------------------------------------------------------------------------------------------

    /**
     * cluster invoke rpc call 临时变量存储
     */
    private class ClusterInvocation {
        /** 当前已重试次数 */
        private AtomicInteger tryTimes = new AtomicInteger(0);
        /** 单次请求曾经fail的service 访问地址 */
        private final Set<HostAndPort> failureHostAndPorts = new HashSet<>();
        /** 暴露给用户的future */
        private final CompletableFuture<Object> consumer = new CompletableFuture<>();

        /** 上次rpc call 的invoker */
        private volatile AsyncInvoker<T> lastInvoker;

        /**
         * 记录上次rpc call 的invoker
         */
        public void rpcCall(AsyncInvoker<T> invoker) {
            this.lastInvoker = invoker;
        }


        /**
         * rpc call failure
         * <p>
         * !!!! 如果是业务抛出异常，是不会重试的
         *
         * @return 是否可以继续重试
         */
        public boolean failure() {
            int newTryTimes = tryTimes.incrementAndGet();
            if (Objects.nonNull(lastInvoker)) {
                failureHostAndPorts.add(HostAndPort.fromString(lastInvoker.url().getAddress()));
            }
            //第一次调用不包含在重试次数里面
            return newTryTimes <= retryTimes;
        }

        /**
         * rpc call完成
         */
        public void done(Object returnObj) {
            consumer.complete(returnObj);
        }

        /**
         * rpc call因异常而结束
         */
        public void doneError(Throwable throwable) {
            consumer.completeExceptionally(throwable);
        }
    }
}
