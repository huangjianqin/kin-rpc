package org.kin.kinrpc.message;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * actor reference
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class ActorRef implements Serializable {
    private static final long serialVersionUID = 3191956547695414179L;
    /** placeholder, means empty sender actor reference */
    public static final ActorRef NO_SENDER = of(ActorAddress.NO_SENDER);

    /** refer actor address */
    private ActorAddress actorAddress;
    /** actor env */
    private transient volatile ActorEnv actorEnv;

    private ActorRef() {
        //更新local RpcEnv
        actorEnv = ActorEnv.current();
    }

    static ActorRef of(ActorAddress actorAddress) {
        ActorRef actorRef = new ActorRef();
        actorRef.actorAddress = actorAddress;
        return actorRef;
    }

    static ActorRef of(ActorAddress actorAddress, ActorEnv actorEnv) {
        if (actorAddress.equals(ActorAddress.NO_SENDER)) {
            return NO_SENDER;
        }
        ActorRef actorRef = new ActorRef();
        actorRef.actorAddress = actorAddress;
        actorRef.actorEnv = actorEnv;
        return actorRef;
    }

    /** 返回actor env, 如果{@link #actorEnv}没有赋值, 则从thread local获取 */
    private ActorEnv actorEnv() {
        //反序列化时没有获取到RpcEnv, 则尝试从执行线程获取RpcEnv
        if (Objects.isNull(actorEnv)) {
            synchronized (this) {
                if (Objects.isNull(actorEnv)) {
                    actorEnv = ActorEnv.current();
                }
            }
        }
        return actorEnv;
    }

    /**
     * 返回actor是否可用
     *
     * @return true表示actor可用
     */
    private boolean isAvailable() {
        return this != NO_SENDER;
    }

    /**
     * 发送消息
     *
     * @param message message
     */
    public void fireAndForget(Serializable message) {
        fireAndForget(message, NO_SENDER);
    }

    /**
     * 发送消息
     *
     * @param message message
     * @param sender  sender actor reference
     */
    public void fireAndForget(Serializable message, ActorRef sender) {
        if (!isAvailable()) {
            return;
        }
        actorEnv().fireAndForget(MessagePayload.requestAndForget(sender.actorAddress, this, message));
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回
     *
     * @param message message
     * @return message response future
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message) {
        return requestResponse(message, NO_SENDER, 0);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     *
     * @param message   message
     * @param timeoutMs send message and receive response message timeout
     * @return message response future
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, long timeoutMs) {
        return requestResponse(message, NO_SENDER, timeoutMs);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回
     *
     * @param message message
     * @param sender  sender actor reference
     * @return message response future
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, ActorRef sender) {
        return requestResponse(message, sender, 0);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     *
     * @param message   message
     * @param sender    sender actor reference
     * @param timeoutMs send message and receive response message timeout
     * @return message response future
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, ActorRef sender, long timeoutMs) {
        if (!isAvailable()) {
            CompletableFuture<R> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }
        return actorEnv().requestResponse(MessagePayload.request(sender.actorAddress, this, message, timeoutMs));
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message  message
     * @param callback message callback
     */
    public void requestResponse(Serializable message, MessageCallback callback) {
        requestResponse(message, callback, NO_SENDER, 0);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message   message
     * @param callback  message callback
     * @param timeoutMs send message and receive response message timeout
     */
    public void requestResponse(Serializable message, MessageCallback callback, long timeoutMs) {
        requestResponse(message, callback, NO_SENDER, timeoutMs);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message  message
     * @param callback message callback
     * @param sender   sender actor reference
     */
    public void requestResponse(Serializable message, MessageCallback callback, ActorRef sender) {
        requestResponse(message, callback, sender, 0);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param message   message
     * @param callback  message callback
     * @param sender    sender actor reference
     * @param timeoutMs send message and receive response message timeout
     */
    public void requestResponse(Serializable message, MessageCallback callback, ActorRef sender, long timeoutMs) {
        if (!isAvailable()) {
            return;
        }
        Preconditions.checkNotNull(callback);
        actorEnv().requestResponse(MessagePayload.request(sender.actorAddress, this, message, timeoutMs))
                .whenComplete((r, t) -> {
                    if (Objects.isNull(t)) {
                        callback.onSuccess(message, r);
                    } else {
                        callback.onFailure(t);
                    }
                });
    }

    //getter
    public ActorAddress getActorAddress() {
        return actorAddress;
    }

    @Override
    public String toString() {
        return "ActorRef{" +
                "actorAddress=" + actorAddress +
                '}';
    }
}
