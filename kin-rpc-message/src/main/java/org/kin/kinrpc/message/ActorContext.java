package org.kin.kinrpc.message;

import org.kin.framework.collection.AttachmentMap;

/**
 * 消息处理上下文
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class ActorContext extends AttachmentMap {
    /** thread local {@link ActorEnv}实例 */
    private static final ThreadLocal<ActorContext> THREAD_LOCAL_ACTOR_CONTEXT = new ThreadLocal<>();

    /** current thread local actor context */
    static ActorContext current() {
        return THREAD_LOCAL_ACTOR_CONTEXT.get();
    }

    /** update current thread local actor context */
    static void update(ActorContext context) {
        THREAD_LOCAL_ACTOR_CONTEXT.set(context);
    }

    /** actor env */
    private final ActorEnv actorEnv;
    /** sender */
    private final ActorRef sender;
    /** receiver actor name */
    private final ActorPath toActorPath;
    /** 消息 */
    private final Object message;
    /** 消息事件时间, 即到达receiver端但还未处理的时间 */
    private long eventTime;
    /** 消息处理时间 */
    private long handleTime;

    public ActorContext(ActorEnv actorEnv,
                        ActorRef sender,
                        ActorPath toActorPath,
                        Object message) {
        this.actorEnv = actorEnv;
        this.sender = sender;
        this.toActorPath = toActorPath;
        this.message = message;
    }

    //setter && getter
    public ActorEnv actorEnv() {
        return actorEnv;
    }

    public ActorRef sender() {
        return sender;
    }

    public ActorPath getFromActorPath() {
        return sender.getActorPath();
    }

    public ActorPath getToActorPath() {
        return toActorPath;
    }

    public Object getMessage() {
        return message;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public long getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(long handleTime) {
        this.handleTime = handleTime;
    }
}
