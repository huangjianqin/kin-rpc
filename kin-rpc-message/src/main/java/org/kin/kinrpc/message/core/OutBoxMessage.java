package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
@SuppressWarnings("rawtypes")
public final class OutBoxMessage implements RpcResponseCallback<Serializable> {
    /** client 发送的消息 */
    private RpcMessage message;
    /** callback */
    private RpcResponseCallback proxy = RpcResponseCallback.EMPTY;

    public OutBoxMessage(RpcMessage message) {
        this.message = message;
    }

    public OutBoxMessage(RpcMessage message, RpcResponseCallback<?> proxy) {
        this.message = message;
        this.proxy = proxy;
    }

    /**
     * 由某个client发送消息
     */
    public void sendWith(TransportClient client) {
        client.send(this);
    }

    public RpcMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "OutBoxMessage{" +
                "message=" + message +
                '}';
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
}
