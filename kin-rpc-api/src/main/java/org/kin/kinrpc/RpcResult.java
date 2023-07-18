package org.kin.kinrpc;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * {@link Invoker#invoke(Invocation)}返回值
 * 服务方法调用结果,
 *
 * @author huangjianqin
 * @date 2023/2/26
 */
public final class RpcResult {
    /** 不包含调用结果的{@link CompletableFuture} */
    private static final CompletableFuture<Object> EMPTY_FUTURE = CompletableFuture.completedFuture(null);

    /** rpc call 元数据 */
    private final Invocation invocation;
    /** result future */
    private CompletableFuture<Object> resultFuture;

    public static RpcResult success(Invocation invocation, CompletableFuture<Object> resultFuture) {
        return new RpcResult(invocation, resultFuture);
    }

    public static RpcResult fail(Invocation invocation, Throwable t) {
        CompletableFuture<Object> exceptionFuture = new CompletableFuture<>();
        exceptionFuture.completeExceptionally(t);
        return new RpcResult(invocation, exceptionFuture);
    }

    public static RpcResult empty(Invocation invocation) {
        return new RpcResult(invocation, EMPTY_FUTURE);
    }

    /**
     * @param invocation   rpc call信息
     * @param resultFuture rpc call result future, 由{@link RpcResult}创建者控制complete
     */
    private RpcResult(Invocation invocation, CompletableFuture<Object> resultFuture) {
        this.invocation = invocation;
        this.resultFuture = resultFuture;
    }

    /**
     * invoke过程是否抛出的异常
     */
    public boolean hasException() {
        return resultFuture.isCompletedExceptionally();
    }

    /**
     * 当invoke结果返回时, 回调{@code callback}
     *
     * @param callback 回调方法
     */
    public RpcResult onFinish(BiConsumer<Object, Throwable> callback) {
        resultFuture = resultFuture.whenComplete(callback);
        return this;
    }

    /**
     * 当invoke结果返回时, complete {@code future}
     * !!! 注意, {@code future}会覆盖{@link #resultFuture}
     *
     * @param future final result future
     */
    public RpcResult onFinish(CompletableFuture<Object> future) {
        resultFuture.whenComplete((r, t) -> completeFuture(r, t, future));
        resultFuture = future;
        return this;
    }

    /**
     * complete {@code future}
     *
     * @param result rpc call结果
     * @param t      rpc call执行异常
     * @param future 需要complete的future
     */
    private void completeFuture(Object result,
                                Throwable t,
                                CompletableFuture<Object> future) {
        if (future.isDone()) {
            return;
        }

        if (Objects.isNull(t)) {
            future.complete(result);
        } else {
            future.completeExceptionally(t);
        }
    }

    /**
     * 当invoke结果返回时, 执行{@code callback}, 然后complete {@code future}
     * !!! 注意, {@code future}会覆盖{@link #resultFuture}
     *
     * @param callback 回调方法
     * @param future   final result future
     */
    public RpcResult onFinish(BiConsumer<Object, Throwable> callback,
                              CompletableFuture<Object> future) {
        resultFuture.whenComplete((r, t) -> callbackAndCompleteFuture(callback, r, t, future));
        resultFuture = future;
        return this;
    }

    /**
     * 执行{@code callback}, 然后complete {@code future}
     *
     * @param callback 回调方法
     * @param result   rpc call结果
     * @param t        rpc call执行异常
     * @param future   需要complete的future
     */
    private void callbackAndCompleteFuture(BiConsumer<Object, Throwable> callback,
                                           Object result,
                                           Throwable t,
                                           CompletableFuture<Object> future) {
        try {
            callback.accept(result, t);
        } finally {
            completeFuture(result, t, future);
        }
    }

    //getter
    public Invocation getInvocation() {
        return invocation;
    }

    public CompletableFuture<Object> getResultFuture() {
        return resultFuture;
    }
}
