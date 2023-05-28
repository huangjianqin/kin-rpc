package org.kin.kinrpc.rpc;

import org.kin.kinrpc.rpc.common.AttributeHolder;
import org.kin.kinrpc.rpc.common.AttributeKey;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 同步invoke返回值, 返回值非future的场景
 * @author huangjianqin
 * @date 2023/2/26
 */
public class CommonResult extends AttributeHolder<CommonResult> implements Result {
    /** invoke真实返回值 */
    private Object result;
    /** invoke过程抛的异常 */
    private Throwable exception;
    /** 附带属性 */
    private final Map<String, Object> attributes = new HashMap<>();

    public CommonResult(Invocation invocation) {
        putAttribute(AttributeKey.INVOCATION, invocation);
    }

    @Nullable
    @Override
    public Object getValue() {
        return result;
    }

    @Override
    public void setValue(Object value) {
        this.result = value;
    }

    @Nullable
    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public void setException(Throwable t) {
        this.exception = t;
    }

    @Override
    public boolean hasException() {
        return Objects.nonNull(exception);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fnc) {
        return CompletableFuture.completedFuture(this).thenApply(fnc);
    }
}
