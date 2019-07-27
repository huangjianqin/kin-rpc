package org.kin.kinrpc.rpc.invoker;


import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncInvoker extends Invoker {
    Future invokeAsync(String methodName, Object... params) throws Throwable;
}
