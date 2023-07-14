package org.kin.kinrpc.message;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    /** future */
    private final CompletableFuture<Object> userFuture;

    OutBoxMessage(MessagePayload payload) {
        this.payload = payload;
        this.timeoutMs = 0;
        this.userFuture = null;
    }

    /** 异步请求调用, 触发future */
    @SuppressWarnings("unchecked")
    OutBoxMessage(MessagePayload payload, CompletableFuture<Object> userFuture, long timeoutMs) {
        this.payload = payload;
        this.timeoutMs = timeoutMs;
        this.userFuture = userFuture;
    }

    /**
     * 由某个client发送消息
     *
     * @param client message client
     */
    void sendWith(MessageClient client) {
        client.send(this);
    }

    /**
     * handle message response
     * {@link ActorEnv#commonExecutors}下执行
     *
     * @param response response message payload
     * @param t        exception when send message or handle message
     */
    void complete(MessagePayload response, Throwable t) {
        if (Objects.isNull(userFuture)) {
            //ignore
            return;
        }

        if (Objects.isNull(t)) {
            userFuture.complete(response.getMessage());
        } else {
            userFuture.completeExceptionally(t);
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
