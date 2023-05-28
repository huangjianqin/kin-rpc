package org.kin.kinrpc.rpc;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 作为{@link Invoker#invoke(Invocation)}方法返回值
 * @author huangjianqin
 * @date 2023/2/26
 */
public interface Result {
    /**
     * invoke结果
     */
    @Nullable
    Object getValue();

    /**
     * 设置invoke结果
     */
    void setValue(Object value);

    /**
     * 获取invoke过程抛出的异常
     */
    @Nullable
    Throwable getException();

    /**
     * 设置invoke过程抛出的异常
     */
    void setException(Throwable t);

    /**
     * invoke过程是否抛出的异常
     */
    boolean hasException();

    /**
     * 用于异步场景
     * 当真实的invoke结果返回时, 回调{@code fnc}
     */
    default <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fnc){
        throw new UnsupportedOperationException("don't support async operation");
    }
}
