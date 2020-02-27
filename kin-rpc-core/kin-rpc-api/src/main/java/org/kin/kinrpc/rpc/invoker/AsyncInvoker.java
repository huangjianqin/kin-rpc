package org.kin.kinrpc.rpc.invoker;


import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncInvoker extends Invoker {
    /**
     * 异步调用rpc
     *
     * @param methodName 方法名
     * @param params     参数
     * @return future
     * @throws Throwable 异常
     */
    Future invokeAsync(String methodName, Object... params) throws Throwable;
}
