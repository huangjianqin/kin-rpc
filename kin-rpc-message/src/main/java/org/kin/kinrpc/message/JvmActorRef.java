package org.kin.kinrpc.message;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
class JvmActorRef extends ActorRef {
    /** actor env */
    private transient ActorEnv actorEnv;

    JvmActorRef(ActorAddress actorAddress) {
        super(actorAddress);
    }

    @Override
    public void doTell(Object message, ActorRef sender) {
        actorEnv.postMessage(new ActorContext(actorEnv, sender, getActorAddress(), message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<R> doAsk(Object message, ActorRef sender, long timeoutMs) {
        FutureActorRef futureSender = new FutureActorRef(sender);
        actorEnv.postMessage(new ActorContext(actorEnv, futureSender, getActorAddress(), message));
        return (CompletableFuture<R>) futureSender.getFuture();
    }
}
