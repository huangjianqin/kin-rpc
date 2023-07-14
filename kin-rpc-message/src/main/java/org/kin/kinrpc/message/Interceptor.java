package org.kin.kinrpc.message;

/**
 * @author huangjianqin
 * @date 2023/7/13
 */
@FunctionalInterface
public interface Interceptor {
    Interceptor ON_RECEIVE = (next, behavior, message) -> behavior.onReceive(message);

    /**
     * intercept
     *
     * @param next     next interceptor chain context
     * @param behavior receiver behavior
     * @param message  receive message
     */
    void intercept(InterceptorChainContext next, Behavior<Object> behavior, Object message);
}
