package org.kin.kinrpc.cluster.call;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class ReactiveResultAdapter implements RpcResultAdapter {
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Override
    public Object convert(Class<?> type, boolean async, CompletableFuture<?> userFuture) {
        //在complete future执行线程(即reference通用线程)执行
        return Mono.fromFuture(userFuture);
    }

    @Override
    public boolean match(Class<?> returnType) {
        return Mono.class.equals(returnType);
    }
}
