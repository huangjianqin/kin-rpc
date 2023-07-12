package org.kin.kinrpc.message;

/**
 * 线程安全的RpcEndpoint
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public abstract class ThreadSafeActor extends Actor {
    public ThreadSafeActor(ActorEnv actorEnv) {
        super(actorEnv);
    }

    @Override
    public final boolean threadSafe() {
        return true;
    }
}
