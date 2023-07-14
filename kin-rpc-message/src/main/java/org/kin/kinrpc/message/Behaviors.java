package org.kin.kinrpc.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/7/13
 */
public final class Behaviors implements Behavior<Serializable> {
    private static final Logger log = LoggerFactory.getLogger(Behaviors.class);

    /** key -> 消息类型, value -> {@link Behavior} */
    private final Map<Class<?>, Behavior<? extends Serializable>> behaviorMap;
    /** interceptor chain */
    private final InterceptorChainContext interceptorChain;

    private Behaviors(Map<Class<?>, Behavior<? extends Serializable>> behaviorMap,
                      List<Interceptor> interceptors) {
        this.behaviorMap = Collections.unmodifiableMap(behaviorMap);
        InterceptorChainContext interceptorChain = InterceptorChainContext.ON_RECEIVE;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptorChain = new InterceptorChainContext(interceptorChain, interceptors.get(i));
        }
        this.interceptorChain = interceptorChain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(ActorContext actorContext, Serializable message) {
        Class<? extends Serializable> messageClass = message.getClass();
        Behavior<? extends Serializable> behavior = behaviorMap.get(messageClass);
        if (Objects.isNull(behavior)) {
            log.warn("can not find behavior for message '{}, make sure it is right'", messageClass.getName());
            return;
        }
        interceptorChain.intercept(null, (Behavior<Serializable>) behavior, actorContext, message);
    }

    //------------------------------------------------------------------------------------------------------------------------builder
    public static Builder builder() {
        return new Builder();
    }

    /** {@link Behaviors} builder */
    public static class Builder {
        /** key -> 消息类型, value -> {@link Behavior} */
        private final Map<Class<?>, Behavior<?>> behaviorMap = new HashMap<>();
        /** interceptor list */
        private final List<Interceptor> interceptors = new ArrayList<>();

        public Builder interceptors(Interceptor... interceptors) {
            return interceptors(Arrays.asList(interceptors));
        }

        public Builder interceptors(Collection<Interceptor> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        public <M extends Serializable> Builder behavior(Class<M> messageClass, Behavior<M> behavior) {
            if (behaviorMap.containsKey(messageClass)) {
                throw new IllegalStateException(String.format("message class '%s' has been register behavior", messageClass.getName()));
            }

            behaviorMap.put(messageClass, behavior);
            return this;
        }

        public Behaviors build() {
            return new Behaviors(behaviorMap, interceptors);
        }
    }
}
