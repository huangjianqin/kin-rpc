package org.kin.kinrpc.message;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 在{@link OutBox}中待发送的消息
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
final class OutBoxMessage {
    /** client 发送的消息 */
    private final MessagePayload payload;
    /** 消息请求超时时间 */
    private final long timeoutMs;
    /** callback */
    private final MessageCallback callback;
    /** future */
    private final CompletableFuture<Serializable> userFuture;

    public OutBoxMessage(MessagePayload payload) {
        this.payload = payload;
        this.timeoutMs = 0;
        this.callback = null;
        this.userFuture = null;
    }

    /** 异步请求调用, 触发future */
    @SuppressWarnings("unchecked")
    OutBoxMessage(MessagePayload payload, CompletableFuture<? extends Serializable> userFuture, long timeoutMs) {
        this.payload = payload;
        this.timeoutMs = timeoutMs;
        this.callback = null;
        this.userFuture = (CompletableFuture<Serializable>) userFuture;
    }

    /** 异步请求调用, 触发callback */
    OutBoxMessage(MessagePayload payload, MessageCallback callback, long timeoutMs) {
        this.payload = payload;
        this.timeoutMs = timeoutMs;
        this.callback = callback;
        this.userFuture = null;
    }

    /**
     * 由某个client发送消息
     */
    void sendWith(MessageClient client) {
        client.send(this);
    }

    /**
     * handle message response
     * {@link ActorEnv#commonExecutors}下执行
     *
     * @param response response message
     * @param t        exception when send message or handle message
     */
    void complete(Serializable response, Throwable t) {
        if (Objects.isNull(t)) {
            if (Objects.nonNull(callback)) {
                //callback
                ExecutorService executor = callback.executor();
                if (Objects.nonNull(executor)) {
                    executor.execute(() ->
                            callback.onResponse(payload.getMessage(), response));
                } else {
                    callback.onResponse(payload.getMessage(), response);
                }
            }
            if (Objects.nonNull(userFuture)) {
                //future
                userFuture.complete(response);
            }
        } else {
            if (Objects.nonNull(callback)) {
                //callback
                ExecutorService executor = callback.executor();
                if (Objects.nonNull(executor)) {
                    executor.execute(() -> callback.onException(t));
                } else {
                    callback.onException(t);
                }
            }
            if (Objects.nonNull(userFuture)) {
                //future
                userFuture.completeExceptionally(t);
            }
        }
    }

    //getter
    MessagePayload getPayload() {
        return payload;
    }

    long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public String toString() {
        return "OutBoxMessage{" +
                "message=" + payload +
                "timeoutMs=" + timeoutMs +
                '}';
    }
}
