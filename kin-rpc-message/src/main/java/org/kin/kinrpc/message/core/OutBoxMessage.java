package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public class OutBoxMessage {
    private RpcMessage message;

    public OutBoxMessage(RpcMessage message) {
        this.message = message;
    }

    public void sendWith(TransportClient client) {
        client.send(message);
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
}
