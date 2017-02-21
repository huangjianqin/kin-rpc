package org.kinrpc.rpc.invoker;

import org.kinrpc.rpc.future.RPCFuture;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncInvoker extends Invoker{
    RPCFuture invokerAsync(String methodName, Object... params);
}
