package org.kin.kinrpc.rpc;

import org.kin.kinrpc.rpc.common.AttributeHolder;
import org.kin.kinrpc.rpc.common.AttributeKey;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 异步invoke返回值
 * @author huangjianqin
 * @date 2023/2/26
 */
public class AsyncResult extends AttributeHolder<AsyncResult> implements Result{
    /** 异步invoke返回的future */
    private CompletableFuture<CommonResult> resultFuture;

    public AsyncResult(Invocation invocation, CompletableFuture<CommonResult> resultFuture) {
        this.resultFuture = resultFuture;
        putAttribute(AttributeKey.INVOCATION, invocation);
    }

    /**
     * 同步等待invoke返回
     */
    @Nullable
    private Result getResult() {
        try {
            if (resultFuture.isDone()) {
                return resultFuture.get();
            }
        } catch (Exception e) {
            throw new RpcException(e);
        }

        // TODO: 2023/2/26  future未完成, 返回null是否合适
        return null;
    }

    @Nullable
    @Override
    public Object getValue() {
        Result result = getResult();
        if (Objects.nonNull(result)) {
            return result.getValue();
        }
        return null;
    }

    @Override
    public void setValue(Object value) {
        try {
            if (resultFuture.isDone()) {
                resultFuture.get().setValue(value);
            }
            else{
                CommonResult result = new CommonResult(getAttribute(AttributeKey.INVOCATION));
                result.setValue(value);
                resultFuture.complete(result);
            }
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    @Nullable
    @Override
    public Throwable getException() {
        Result result = getResult();
        if (Objects.nonNull(result)) {
            return result.getException();
        }
        return null;
    }

    @Override
    public void setException(Throwable t) {
        try {
            if (resultFuture.isDone()) {
                resultFuture.get().setException(t);
            }
            else{
                CommonResult result = new CommonResult(getAttribute(AttributeKey.INVOCATION));
                result.setException(t);
                resultFuture.complete(result);
            }
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    @Override
    public boolean hasException() {
        Result result = getResult();
        return Objects.nonNull(result) && result.hasException();
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fnc) {
        return resultFuture.thenApply(fnc);
    }
}
