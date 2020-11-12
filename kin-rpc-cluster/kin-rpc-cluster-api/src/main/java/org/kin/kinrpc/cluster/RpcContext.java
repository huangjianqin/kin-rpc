package org.kin.kinrpc.cluster;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2020/11/11
 */
public class RpcContext {
    private static ThreadLocal<CompletableFuture> localFuture = new ThreadLocal<>();

    public static <T> CompletableFuture<T> future() {
        if (Objects.nonNull(localFuture)) {
            return localFuture.get();
        }

        return null;
    }

    static void updateFuture(CompletableFuture<?> future) {
        localFuture.set(future);
    }
}
