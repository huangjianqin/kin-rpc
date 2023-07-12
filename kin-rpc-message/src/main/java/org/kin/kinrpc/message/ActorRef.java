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
    /** remote address */
    private ActorAddress address;
    /** rpc环境 */
    private transient volatile ActorEnv actorEnv;

    public ActorRef() {
        //更新local RpcEnv
        actorEnv = ActorEnv.current();
    }

    public static ActorRef of(ActorAddress actorAddress) {
        ActorRef actorRef = new ActorRef();
        actorRef.address = actorAddress;
        return actorRef;
    }

    public static ActorRef of(ActorAddress actorAddress, ActorEnv actorEnv) {
        ActorRef actorRef = ActorRef.of(actorAddress);
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
     * 封装成{@link MessagePayload}
     */
    private MessagePayload toPayload(Serializable message, boolean ignoreResponse) {
        return MessagePayload.request(actorEnv().getListenAddress(), this, message, ignoreResponse);
    }

    /**
     * 发送消息
     */
    public void fireAndForget(Serializable message) {
        actorEnv().fireAndForget(toPayload(message, true));
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message) {
        return actorEnv().requestResponse(toPayload(message, false));
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完成并返回, 并且支持超时
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, long timeoutMs) {
        return actorEnv().requestResponse(toPayload(message, false), timeoutMs);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param callback 自定义callback
     */
    public void requestResponse(Serializable message, MessageCallback callback) {
        Preconditions.checkNotNull(callback);
        actorEnv().requestResponse(toPayload(message, false), callback);
    }

    /**
     * 发送消息, 响应时回调{@code callback}, 并且支持超时
     *
     * @param callback  自定义callback
     * @param timeoutMs 超时时间
     */
    public void requestResponse(Serializable message, MessageCallback callback, long timeoutMs) {
        Preconditions.checkNotNull(callback);
        actorEnv().requestResponse(toPayload(message, false), callback, timeoutMs);
    }

    //getter
    public ActorAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ActorRef{" +
                "address=" + address +
                '}';
    }
}
