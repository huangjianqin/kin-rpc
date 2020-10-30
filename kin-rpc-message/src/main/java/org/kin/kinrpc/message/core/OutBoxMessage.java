package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class OutBoxMessage implements RpcResponseCallback<Serializable> {
    private RpcMessage message;
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

    @Override
    public void onSuccess(Serializable message) {
        proxy.onSuccess(message);
    }

    @Override
    public void onFail(Throwable e) {
        proxy.onFail(e);
    }
}
