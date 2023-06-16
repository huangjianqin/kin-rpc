package org.kin.kinrpc.cluster;

import org.kin.kinrpc.rpc.RpcCallReturnAdapter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2021/2/6
 */
public class FutureReturnAdapter implements RpcCallReturnAdapter {
    @Override
    public Object handleReturn(Class<?> returnType, boolean asyncRpcCall, CompletableFuture<?> future) {
        //直接返回
        return future;
    }

    @Override
    public boolean match(Class<?> returnType) {
        return Future.class.equals(returnType) || CompletableFuture.class.equals(returnType);
    }
}
