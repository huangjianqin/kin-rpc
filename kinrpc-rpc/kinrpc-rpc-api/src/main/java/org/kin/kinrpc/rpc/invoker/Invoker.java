package org.kin.kinrpc.rpc.invoker;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker {
    Object invoke(String methodName, Object... params);
}
