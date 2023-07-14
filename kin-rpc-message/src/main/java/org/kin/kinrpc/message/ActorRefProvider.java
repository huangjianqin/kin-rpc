package org.kin.kinrpc.message;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public interface ActorRefProvider<E extends ActorEnv> {
    ActorRefProvider<ActorEnv> LOCAL = (actorEnv, actorAddress) -> new JvmActorRef(actorAddress);

    /**
     * 返回{@link ActorRef}实例
     *
     * @param actorEnv     actor env
     * @param actorAddress actor address
     * @return {@link ActorRef}实例
     */
    ActorRef actorOf(E actorEnv, ActorAddress actorAddress);
}
