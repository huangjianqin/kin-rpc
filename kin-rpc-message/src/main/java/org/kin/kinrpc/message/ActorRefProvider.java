package org.kin.kinrpc.message;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public interface ActorRefProvider<E extends ActorEnv> {
    ActorRefProvider<ActorEnv> LOCAL = (actorEnv, actorPath) -> new LocalActorRef(actorPath, actorEnv);

    /**
     * 返回{@link ActorRef}实例
     *
     * @param actorEnv  actor env
     * @param actorPath actor path
     * @return {@link ActorRef}实例
     */
    ActorRef actorOf(E actorEnv, ActorPath actorPath);
}
