package org.kin.kinrpc.message;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * actor reference
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class ActorRef {
    /** placeholder, means empty sender actor reference */
    public static final ActorRef NO_SENDER = new ActorRef(ActorPath.NO_SENDER) {
    };
    /** completed future */
    private static final CompletableFuture<Object> COMPLETED_FUTURE = new CompletableFuture<>();

    static {
        COMPLETED_FUTURE.complete(null);
    }

    /** refer actor path */
    private final ActorPath actorPath;

    protected ActorRef(ActorPath actorPath) {
        this.actorPath = actorPath;
    }

    /**
     * 返回actor是否可用
     *
     * @return true表示actor可用
     */
    private final boolean isAvailable() {
        return this != NO_SENDER;
    }

    /**
     * 发送消息
     *
     * @param message message
     */
    public final void tell(Object message) {
        tell(message, NO_SENDER);
    }

    /**
     * 发送消息
     *
     * @param message message
     * @param sender  sender actor reference
     */
    public final void tell(Object message, ActorRef sender) {
        if (!isAvailable()) {
            return;
        }
        doTell(message, sender);
    }

    /**
     * 发送消息
     *
     * @param message message
     * @param sender  sender actor reference
     */
    public void doTell(Object message, ActorRef sender) {
        //default do nothing
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回
     *
     * @param message message
     * @return message response future
     */
    public final <R> CompletableFuture<R> ask(Object message) {
        return ask(message, NO_SENDER, 0);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     *
     * @param message   message
     * @param timeoutMs send message and receive response message timeout
     * @return message response future
     */
    public final <R> CompletableFuture<R> ask(Object message, long timeoutMs) {
        return ask(message, NO_SENDER, timeoutMs);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回
     *
     * @param message message
     * @param sender  sender actor reference
     * @return message response future
     */
    public final <R> CompletableFuture<R> ask(Object message, ActorRef sender) {
        return ask(message, sender, 0);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     *
     * @param message   message
     * @param sender    sender actor reference
     * @param timeoutMs send message and receive response message timeout
     * @return message response future
     */
    public final <R> CompletableFuture<R> ask(Object message, ActorRef sender, long timeoutMs) {
        if (!isAvailable()) {
            CompletableFuture<R> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }
        return doAsk(message, sender, timeoutMs);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     *
     * @param message   message
     * @param sender    sender actor reference
     * @param timeoutMs send message and receive response message timeout
     * @return message response future
     */
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> doAsk(Object message, ActorRef sender, long timeoutMs) {
        //default do nothing
        return (CompletableFuture<R>) COMPLETED_FUTURE;
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message  message
     * @param callback message callback
     */
    public final void ask(Object message, MessageCallback callback) {
        ask(message, callback, NO_SENDER, 0);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message   message
     * @param callback  message callback
     * @param timeoutMs send message and receive response message timeout
     */
    public final void ask(Object message, MessageCallback callback, long timeoutMs) {
        ask(message, callback, NO_SENDER, timeoutMs);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message  message
     * @param callback message callback
     * @param sender   sender actor reference
     */
    public final void ask(Object message, MessageCallback callback, ActorRef sender) {
        ask(message, callback, sender, 0);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message   message
     * @param callback  message callback
     * @param sender    sender actor reference
     * @param timeoutMs send message and receive response message timeout
     */
    public final void ask(Object message, MessageCallback callback, ActorRef sender, long timeoutMs) {
        if (!isAvailable()) {
            return;
        }
        Preconditions.checkNotNull(callback);
        ask(message, sender, timeoutMs)
                .whenComplete((r, t) -> {
                    if (Objects.isNull(t)) {
                        callback.onSuccess(message, r);
                    } else {
                        callback.onFailure(t);
                    }
                });
    }

    /**
     * response message
     *
     * @param message response message
     */
    public void answer(Object message) {
        //default donothing
    }

    //getter
    public final ActorPath getActorPath() {
        return actorPath;
    }

    public final boolean isLocal() {
        return Address.LOCAL.equals(getActorPath().getAddress());
    }

    @Override
    public String toString() {
        return "ActorRef{" +
                "actorPath=" + actorPath +
                '}';
    }
}
