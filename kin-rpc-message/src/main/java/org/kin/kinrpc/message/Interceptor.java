package org.kin.kinrpc.message;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/13
 */
@FunctionalInterface
public interface Interceptor {
    Interceptor ON_RECEIVE = (next, behavior, actorContext, message) -> behavior.onReceive(actorContext, message);

    /**
     * intercept
     *
     * @param next         next interceptor chain context
     * @param behavior     receiver behavior
     * @param actorContext actor context
     * @param message      receive message
     */
    void intercept(InterceptorChainContext next, Behavior<Serializable> behavior, ActorContext actorContext, Serializable message);
}
