package org.kin.kinrpc;

/**
 * remoting service服务方法调用结果, 用于{@link Interceptor#onResponse(Invocation, RpcResponse)}
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

    //setter && getter
    public Object getResult() {
        return result;
    }

    public RpcResponse result(Object result) {
        this.result = result;
        return this;
    }

    public Throwable getException() {
        return exception;
    }

    public RpcResponse t(Throwable t) {
        this.exception = t;
        return this;
    }
}
