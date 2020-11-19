package org.kin.kinrpc.cluster;

import java.util.concurrent.CompletableFuture;

/**
 * 用于reference 获取future, 用于异步操作
 *
 * @author huangjianqin
 * @date 2020/11/11
 */
public class RpcContext {
    private static ThreadLocal<CompletableFuture> future = new ThreadLocal<>();

    /**
     * 获取future
     */
    public static <T> CompletableFuture<T> future() {
        return future.get();
    }

    /**
     * 设置future
     */
    static void updateFuture(CompletableFuture<?> future) {
        RpcContext.future.set(future);
    }
}
