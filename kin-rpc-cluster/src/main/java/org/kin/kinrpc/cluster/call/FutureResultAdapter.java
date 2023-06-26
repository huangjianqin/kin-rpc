package org.kin.kinrpc.cluster.call;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2021/2/6
 */
public class FutureResultAdapter implements RpcResultAdapter {
    @Override
    public Object convert(Class<?> type, boolean async, CompletableFuture<?> userFuture) {
        //直接返回userFuture
        return userFuture;
    }

    @Override
    public boolean match(Class<?> returnType) {
        //返回值是future
        return Future.class.equals(returnType) || CompletableFuture.class.equals(returnType);
    }
}
