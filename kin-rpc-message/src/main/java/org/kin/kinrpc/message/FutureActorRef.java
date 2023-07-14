package org.kin.kinrpc.message;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
class FutureActorRef extends DelegateActorRef {
    /** ask future */
    private final CompletableFuture<Object> future = new CompletableFuture<>();

    FutureActorRef(ActorRef proxy) {
        super(proxy);
    }

    @Override
    public final void answer(Object message) {
        future.complete(message);
    }

    //getter
    CompletableFuture<Object> getFuture() {
        return future;
    }
}
