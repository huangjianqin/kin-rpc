package org.kin.kinrpc.transport.rsocket;

import org.kin.kinrpc.core.RpcCallReturnAdapter;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Reactor类型返回值处理
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
public class ReactiveReturnAdapter implements RpcCallReturnAdapter {
    @Override
    public Object handleReturn(Class<?> returnType, boolean asyncRpcCall, CompletableFuture<?> future) throws ExecutionException, InterruptedException {
        try {
            //直接通过future获得Publisher
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw e;
        }
    }

    @Override
    public boolean match(Class<?> returnType) {
        return Publisher.class.isAssignableFrom(returnType);
    }
}