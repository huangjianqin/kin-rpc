package org.kin.kinrpc.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixInvokable;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.fallback.FallbackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/8/8
 */
public class KinRpcHystrixCommand extends HystrixCommand<RpcResult> implements HystrixInvokable<RpcResult> {
    private static final Logger log = LoggerFactory.getLogger(KinRpcHystrixCommand.class);
    /** hystrix command等待执行锁默认超时时间 */
    private static final long DEFAULT_LOCK_TIMEOUT = 1000;

    /** rpc call invoker */
    private final Invoker<?> invoker;
    /** rpc call info */
    private final Invocation invocation;
    /** hystrix command等待执行锁 */
    private final CountDownLatch lock = new CountDownLatch(1);
    /** hystrix command执行时间流 */
    private final List<AsyncHystrixEvent> events = new ArrayList<>();

    public KinRpcHystrixCommand(Invoker<?> invoker, Invocation invocation) {
        super(SetterFactory.getSuitableSetterFactory().createSetter(invocation));
        this.invoker = invoker;
        this.invocation = invocation;
    }

    /**
     * invoke hystrix command
     */
    public RpcResult invoke() {
        if (isCircuitBreakerOpen() && log.isDebugEnabled()) {
            log.debug("circuit breaker is opened, invocation={}", invocation);
        }
        Future<RpcResult> queueFuture = queue();
        try {
            boolean finished = lock.await(getLockTimeout(), TimeUnit.MILLISECONDS);
            if (!finished && !this.isExecutionComplete()) {
                throw new RpcTimeoutException(String.format("wait hystrix command to run timeout. events=%s, invocation=%s", getExecutionEventsString(), invocation));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            return queueFuture.get();
        } catch (InterruptedException e) {
            throw new RpcException(String.format("hystrix command run interrupted. events=%s, invocation=%s", getExecutionEventsString(), invocation));
        } catch (ExecutionException e) {
            throw new RpcException(String.format("hystrix command run fail. events=%s, invocation=%s", getExecutionEventsString(), invocation), e.getCause());
        }
    }

    @Override
    protected RpcResult run() {
        events.add(AsyncHystrixEvent.EMIT);
        RpcResult rpcResult = invoker.invoke(invocation);
        lock.countDown();
        events.add(AsyncHystrixEvent.INVOKE_SUCCESS);
        return rpcResult;
    }

    @Override
    protected RpcResult getFallback() {
        events.add(AsyncHystrixEvent.FALLBACK_EMIT);
        if (lock.getCount() > 0) {
            // > 0 说明run方法没有执行, 或是执行时立刻失败了
            lock.countDown();
            events.add(AsyncHystrixEvent.FALLBACK_UNLOCKED);
        }
        try {
            return FallbackManager.onFallback(invocation, new RpcExceptionBlockException(String.format("rpc call blocked, invocation=%s", invocation)));
        } finally {
            events.add(AsyncHystrixEvent.FALLBACK_SUCCESS);
        }
    }

    /**
     * 返回hystrix command执行等待时间
     */
    private long getLockTimeout() {
        //使用rpc call timeout
        MethodConfig methodConfig = invocation.attachment(InvocationConstants.METHOD_CONFIG_KEY);
        if (Objects.nonNull(methodConfig) &&
                methodConfig.getTimeout() > 0) {
            return methodConfig.getTimeout();
        }
        return DEFAULT_LOCK_TIMEOUT;
    }

    /**
     * 返回hystrix events stream description
     */
    private String getExecutionEventsString() {
        List<HystrixEventType> executionEvents = getExecutionEvents();
        if (executionEvents == null) {
            executionEvents = Collections.emptyList();
        }
        StringBuilder message = new StringBuilder("[");
        //hystrix官方event
        for (HystrixEventType executionEvent : executionEvents) {
            message.append(HystrixEventType.class.getSimpleName()).append("#").append(executionEvent.name())
                    .append(",");
        }
        //自定义event
        for (AsyncHystrixEvent event : events) {
            message.append(AsyncHystrixEvent.class.getSimpleName()).append("#").append(event.name()).append(",");
        }
        if (message.length() > 1) {
            message.deleteCharAt(message.length() - 1);
        }
        return message.append("]").toString();
    }

}
