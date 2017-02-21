package org.kinrpc.rpc.invoker;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker {
    Object invoke(String methodName, Object... params);
}
