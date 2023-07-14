package org.kin.kinrpc.message;

/**
 * actor抽象
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class Actor {
    /** 该{@link Actor}实例对应的{@link ActorRef}实例 */
    private ActorRef self;

    /**
     * actor started
     */
    protected void onStart() {
        //default do nothing
    }

    /**
     * actor stopped
     */
    protected void onStop() {
        //default do nothing
    }

    /**
     * user定义消息处理behavior
     */
    protected abstract Behaviors createBehaviors();

    /**
     * 标识是否线程安全actor, 即消息处理是否线程安全
     */
    public boolean threadSafe() {
        //default
        return false;
    }

    /**
     * 返回处理中的消息所属sender
     *
     * @return sender
     */
    public final ActorRef sender() {
        return ActorContext.current().sender();
    }

    /**
     * 返回该actor对应的reference
     */
    public final ActorRef self() {
        return self;
    }

    /**
     * 内部使用, 设置该{@link Actor}实例对应的{@link ActorRef}实例
     *
     * @param self {@link ActorRef}实例
     */
    final void internalInit(ActorRef self) {
        this.self = self;
    }
}
