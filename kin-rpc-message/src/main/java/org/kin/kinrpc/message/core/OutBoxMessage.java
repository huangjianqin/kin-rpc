package org.kin.kinrpc.message.core;

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
    /** callback */
    private final RpcResponseCallback callback;
    /** 消息请求超时时间 */
    private final long timeoutMs;

    /**
     * 同步请求调用
     */
    OutBoxMessage(RpcMessage rpcMessage) {
        this(rpcMessage, RpcResponseCallback.EMPTY, 0);
    }

    /**
     * 异步请求调用
     */
    OutBoxMessage(RpcMessage rpcMessage, RpcResponseCallback callback, long timeoutMs) {
        this.rpcMessage = rpcMessage;
        this.callback = callback;
        //默认隐藏超时时间1分钟, 如果由于异常不能返回, 但也没有设置超时, 会导致程序缓存大量Future, 故设置隐藏超时时间, 以便在该场景下释放无用对象实例
        this.timeoutMs = timeoutMs == 0 ? TimeUnit.MINUTES.toMillis(1) : timeoutMs;
    }

    /**
     * 由某个client发送消息
     */
    void sendWith(TransportClient client) {
        client.send(this);
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

    @Override
    public String toString() {
        return "OutBoxMessage{" +
                "message=" + rpcMessage +
                '}';
    }
}
