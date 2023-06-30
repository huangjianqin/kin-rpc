package org.kin.kinrpc;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 用于服务方法异步任务执行返回结果
 * like javax.servlet.AsyncContext in the Servlet 3.0
 * <p>
 * 使用方法:
 * 1. {@link AsyncContext#start()}获取{@link AsyncContext}实例
 * 2. 在异步任务执行完成时, 调用{@link #complete(Object)}, 或者异步任务执行异常时, 调用{@link #completeExceptionally(Throwable)}
 *
 * @author huangjianqin
 * @date 2020/11/19
 */
public class AsyncContext {
    /** thread local {@link AsyncContext}实例 */
    private static final ThreadLocal<AsyncContext> ASYNC_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * start async context
     * @return {@link AsyncContext}实例
     */
    public static AsyncContext start() {
        AsyncContext context = ASYNC_CONTEXT_THREAD_LOCAL.get();
        if (Objects.nonNull(context)) {
            throw new IllegalStateException("async context has been started");
        }

        context = new AsyncContext();
        ASYNC_CONTEXT_THREAD_LOCAL.set(context);
        return context;
    }

    /**
     * return and remove current async context
     *
     * @return current async context
     */
    @Nullable
    public static AsyncContext remove() {
        AsyncContext context = ASYNC_CONTEXT_THREAD_LOCAL.get();
        ASYNC_CONTEXT_THREAD_LOCAL.remove();
        return context;
    }

    /** result future */
    private final CompletableFuture<Object> future = new CompletableFuture<>();

    /**
     * complete async context with given result {@code obj}
     *
     * @param obj 异步任务执行结果
     */
    public void complete(Object obj) {
        if (future.isDone()) {
            throw new IllegalStateException("async context is has been completed");
        }

        future.complete(obj);
    }

    /**
     * complete async context with given exception {@code t}
     *
     * @param t 异常
     */
    public void completeExceptionally(Throwable t) {
        if (future.isDone()) {
            throw new IllegalStateException("async context is has been completed");
        }

        future.completeExceptionally(t);
    }

    //getter
    public CompletableFuture<Object> getFuture() {
        return future;
    }
}
