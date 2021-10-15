package org.kin.kinrpc.message.core;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 在{@link OutBox}中等待client发送的消息
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
final class OutBoxMessage {
    /** client 发送的消息 */
    private final RpcMessage rpcMessage;
    /** 消息请求超时时间 */
    private final long timeoutMs;
    /** callback */
    private final RpcResponseCallback callback;
    /** complete使用者future */
    private final CompletableFuture<? extends Serializable> source;

    public OutBoxMessage(RpcMessage rpcMessage) {
        this.rpcMessage = rpcMessage;
        this.timeoutMs = 0;
        this.callback = null;
        this.source = null;
    }

    /**
     * 同步请求调用
     */
    OutBoxMessage(RpcMessage rpcMessage, CompletableFuture<? extends Serializable> source, long timeoutMs) {
        this.rpcMessage = rpcMessage;
        //默认隐藏超时时间1分钟, 如果由于异常不能返回, 但也没有设置超时, 会导致程序缓存大量Future, 故设置隐藏超时时间, 以便在该场景下释放无用对象实例
        this.timeoutMs = timeoutMs == 0 ? TimeUnit.MINUTES.toMillis(1) : timeoutMs;
        this.callback = null;
        this.source = source;
    }

    /**
     * 异步请求调用
     */
    OutBoxMessage(RpcMessage rpcMessage, RpcResponseCallback callback, long timeoutMs) {
        this.rpcMessage = rpcMessage;
        //默认隐藏超时时间1分钟, 如果由于异常不能返回, 但也没有设置超时, 会导致程序缓存大量Future, 故设置隐藏超时时间, 以便在该场景下释放无用对象实例
        this.timeoutMs = timeoutMs == 0 ? TimeUnit.MINUTES.toMillis(1) : timeoutMs;
        this.callback = callback;
        this.source = null;
    }

    /**
     * 由某个client发送消息
     */
    void sendWith(TransportClient client) {
        client.send(this);
    }

    /**
     * 是否是触发callback
     */
    public boolean isExecCallback() {
        return Objects.nonNull(callback);
    }

    /**
     * 是否是complete future
     */
    public boolean isCompleteFuture() {
        return Objects.nonNull(source);
    }

    //getter
    RpcMessage getRpcMessage() {
        return rpcMessage;
    }

    long getTimeoutMs() {
        return timeoutMs;
    }

    RpcResponseCallback getCallback() {
        return callback;
    }

    @SuppressWarnings("rawtypes")
    CompletableFuture getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "OutBoxMessage{" +
                "message=" + rpcMessage +
                '}';
    }
}
