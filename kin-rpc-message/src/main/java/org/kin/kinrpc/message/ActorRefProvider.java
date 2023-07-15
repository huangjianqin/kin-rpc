package org.kin.kinrpc.message;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public interface ActorRefProvider<E extends ActorEnv> {
    ActorRefProvider<ActorEnv> LOCAL = new ActorRefProvider<ActorEnv>() {
        @Override
        public ActorRef actorOf(ActorEnv actorEnv, ActorPath actorPath) {
            return new LocalActorRef(actorPath, actorEnv);
        }

        @Override
        public ActorRef actorOf(ActorEnv actorEnv, String actorName) {
            return new LocalActorRef(ActorPath.of(Address.LOCAL, actorName), actorEnv);
        }
    };

    /**
     * 用于{@link ActorEnv#actorOf(Address, String)}返回{@link ActorRef}实例
     *
     * @param actorEnv  actor env
     * @param actorPath actor path
     * @return {@link ActorRef}实例
     */
    ActorRef actorOf(E actorEnv, ActorPath actorPath);

    /**
     * 用于{@link ActorEnv#newActor(String, Actor)}返回{@link ActorRef}实例
     *
     * @param actorEnv  actor env
     * @param actorName actor name
     * @return {@link ActorRef}实例
     */
    ActorRef actorOf(E actorEnv, String actorName);
}
