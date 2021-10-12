package org.kin.kinrpc.message.core;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 在{@link OutBox}中等待client发送的消息
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
@SuppressWarnings("rawtypes")
final class OutBoxMessage implements RpcResponseCallback<Serializable> {
    /** client 发送的消息 */
    private final RpcMessage message;
    /** callback */
    private final RpcResponseCallback proxy;
    /** 消息请求超时时间 */
    private final long timeoutMs;

    /**
     * 同步请求调用
     */
    OutBoxMessage(RpcMessage message) {
        this(message, RpcResponseCallback.EMPTY, 0);
    }

    /**
     * 异步请求调用
     */
    OutBoxMessage(RpcMessage message, RpcResponseCallback<?> proxy, long timeoutMs) {
        this.message = message;
        this.proxy = proxy;
        //默认隐藏超时时间1分钟, 如果由于异常不能返回, 但也没有设置超时, 会导致程序缓存大量Future, 故设置隐藏超时时间, 以便在该场景下释放无用对象实例
        this.timeoutMs = timeoutMs == 0 ? TimeUnit.MINUTES.toMillis(1) : timeoutMs;
    }

    /**
     * 由某个client发送消息
     */
    void sendWith(TransportClient client) {
        client.send(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(Serializable message) {
        //最终调用callback的地方
        proxy.onSuccess(message);
    }

    @Override
    public void onFail(Throwable e) {
        //最终调用callback的地方
        proxy.onFail(e);
    }

    //getter
    RpcMessage getMessage() {
        return message;
    }

    long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public String toString() {
        return "OutBoxMessage{" +
                "message=" + message +
                '}';
    }
}
