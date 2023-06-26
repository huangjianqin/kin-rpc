package org.kin.kinrpc;

import java.util.concurrent.CompletableFuture;

/**
 * 当服务方法为异步调用时, 向user暴露接口获取future
 *
 * @author huangjianqin
 * @date 2020/11/11
 */
public class RpcContext {
    private static final ThreadLocal<CompletableFuture<?>> FUTURE = new ThreadLocal<>();

    /**
     * 获取async rpc call关联future
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> future() {
        return (CompletableFuture<T>) FUTURE.get();
    }

    /**
     * 更新async rpc call关联future
     */
    public static void updateFuture(CompletableFuture<?> future) {
        RpcContext.FUTURE.set(future);
    }
}
