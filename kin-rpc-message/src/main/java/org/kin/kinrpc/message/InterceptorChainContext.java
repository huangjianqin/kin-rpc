package org.kin.kinrpc.message;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/13
 */
public class InterceptorChainContext implements Interceptor {
    /** tail interceptor context in chain */
    private static final InterceptorChainContext TAIL = new InterceptorChainContext(null, null);
    /** second last interceptor context in chain, main to call {@link Behavior#onReceive(ActorContext, Object)} */
    public static final InterceptorChainContext ON_RECEIVE = new InterceptorChainContext(TAIL, Interceptor.ON_RECEIVE);


    /** next interceptor behavior */
    private final InterceptorChainContext next;
    /** interceptor */
    private final Interceptor interceptor;

    public InterceptorChainContext(InterceptorChainContext next, Interceptor interceptor) {
        this.next = next;
        this.interceptor = interceptor;
    }

    @Override
    public void intercept(InterceptorChainContext next, Behavior<Object> behavior, Object message) {
        if (Objects.isNull(interceptor)) {
            return;
        }

        interceptor.intercept(this.next, behavior, message);
    }
}
