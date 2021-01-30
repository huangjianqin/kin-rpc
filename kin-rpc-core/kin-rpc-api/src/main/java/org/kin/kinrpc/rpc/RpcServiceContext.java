package org.kin.kinrpc.rpc;

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * 用于provider service 接口方法异步返回结果
 *
 * @author huangjianqin
 * @date 2020/11/19
 */
public class RpcServiceContext {
    private static ThreadLocal<Future<?>> future = new ThreadLocal<>();

    /**
     * 设置future
     */
    public static void updateFuture(Future<?> future) {
        RpcServiceContext.future.set(future);
    }

    /**
     * 获取future
     */
    @SuppressWarnings("unchecked")
    public static <T> Future<T> future() {
        return (Future<T>) future.get();
    }

    /**
     * provider service 是否异步返回
     */
    public static boolean asyncReturn() {
        return Objects.nonNull(future.get());
    }

    /**
     * 移除future
     */
    public static void reset() {
        future.remove();
    }
}
