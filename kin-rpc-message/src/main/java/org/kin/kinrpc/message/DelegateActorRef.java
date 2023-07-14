package org.kin.kinrpc.message;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
class DelegateActorRef extends ActorRef {
    /** actor ref proxy */
    private final ActorRef proxy;

    DelegateActorRef(ActorRef proxy) {
        super(proxy.getActorAddress());
        this.proxy = proxy;
    }

    @Override
    public void doTell(Object message, ActorRef sender) {
        proxy.doTell(message, sender);
    }

    @Override
    public <R> CompletableFuture<R> doAsk(Object message, ActorRef sender, long timeoutMs) {
        return proxy.doAsk(message, sender, timeoutMs);
    }

    @Override
    public void answer(Object message) {
        proxy.answer(message);
    }

    //getter
    ActorRef getProxy() {
        return proxy;
    }
}
