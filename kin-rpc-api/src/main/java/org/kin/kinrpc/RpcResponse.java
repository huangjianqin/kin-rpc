package org.kin.kinrpc;

import java.util.Objects;

/**
 * 服务方法调用结果, 用于{@link Filter#onResponse(Invocation, RpcResponse)}
 *
 * @author huangjianqin
 * @date 2023/7/3
 */
public class RpcResponse {
    /** rpc response result */
    private Object result;
    /** rpc response exception */
    private Throwable exception;

    public RpcResponse(Object result, Throwable exception) {
        this.result = result;
        this.exception = exception;
    }

    /**
     * remoting层rpc请求响应是否异常
     */
    public boolean hasException() {
        return Objects.nonNull(exception);
    }

    //setter && getter
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
