package org.kin.kinrpc.message.core;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class OutBoxMessage implements RpcResponseCallback<Serializable> {
    /** callback executor */
    private static final ExecutionContext CALLBACK_EXECUTORS = ExecutionContext.elastic(2, Math.max(2, SysUtils.CPU_NUM), "kinrpc-message-callback");

    static {
        JvmCloseCleaner.DEFAULT().add(CALLBACK_EXECUTORS::shutdown);
    }

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
        CALLBACK_EXECUTORS.execute(() -> proxy.onSuccess(message));
    }

    @Override
    public void onFail(Throwable e) {
        //最终调用callback的地方
        CALLBACK_EXECUTORS.execute(() -> proxy.onFail(e));
    }
}
