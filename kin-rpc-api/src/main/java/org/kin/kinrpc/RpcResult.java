package org.kin.kinrpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * 作为{@link Invoker#invoke(Invocation)}方法返回值
 *
 * @author huangjianqin
 * @date 2023/2/26
 */
public final class RpcResult {
    /** 不包含调用结果的{@link CompletableFuture} */
    private static final CompletableFuture<Object> EMPTY_FUTURE = CompletableFuture.completedFuture(null);

    /** rpc call 元数据 */
    private final Invocation invocation;
    /** invoke返回的future */
    private final CompletableFuture<Object> resultFuture;

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
     * 当invoke结果返回时, 回调{@code fnc}
     *
     * @param action 回调方法
     */
    public CompletableFuture<Object> onFinish(BiConsumer<Object, Throwable> action) {
        return resultFuture.whenComplete(action);
    }

    /**
     * 当invoke结果返回时, 异步回调{@code fnc}
     *
     * @param action   回调方法
     * @param executor 回调方法执行线程
     */
    public CompletableFuture<Object> onFinishAsync(BiConsumer<Object, Throwable> action,
                                                   Executor executor) {
        return resultFuture.whenCompleteAsync(action, executor);
    }
}