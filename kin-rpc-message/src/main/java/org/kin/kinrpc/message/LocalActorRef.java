package org.kin.kinrpc.message;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
class LocalActorRef extends ActorRef {
    /** actor env */
    private transient ActorEnv actorEnv;

    LocalActorRef(ActorPath actorPath, ActorEnv actorEnv) {
        super(actorPath);
        this.actorEnv = actorEnv;
    }

    @Override
    public void doTell(Object message, ActorRef sender) {
        actorEnv.postMessage(new ActorContext(actorEnv, sender, getActorPath(), message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<R> doAsk(Object message, ActorRef sender, long timeoutMs) {
        FutureActorRef futureSender = new FutureActorRef(sender);
        actorEnv.postMessage(new ActorContext(actorEnv, futureSender, getActorPath(), message));
        return (CompletableFuture<R>) futureSender.getFuture();
    }
}
