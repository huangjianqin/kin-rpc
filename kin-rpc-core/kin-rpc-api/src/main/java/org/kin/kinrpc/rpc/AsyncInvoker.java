package org.kin.kinrpc.rpc;


import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncInvoker<T> extends Invoker<T> {
    /**
     * 异步调用rpc
     *
     * @param methodName 方法名
     * @param params     参数
     * @return future
     */
    Future invokeAsync(String methodName, Object... params);
}
