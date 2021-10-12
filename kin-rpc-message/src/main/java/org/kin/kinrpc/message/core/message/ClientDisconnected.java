package org.kin.kinrpc.message.core.message;

import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;

import java.io.Serializable;

/**
 * 内置消息, client断开连接
 *
 * @author huangjianqin
 * @date 2020/8/2
 */
public final class ClientDisconnected implements Serializable {
    private static final long serialVersionUID = 5344051735870164432L;

    private KinRpcAddress rpcAddress;

    public static ClientDisconnected of(KinRpcAddress rpcAddress) {
        ClientDisconnected msg = new ClientDisconnected();
        msg.rpcAddress = rpcAddress;
        return msg;
    }

    //setter && getter
    public KinRpcAddress getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(KinRpcAddress rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    @Override
    public String toString() {
        return "ClientDisconnected{" +
                "rpcAddress=" + rpcAddress +
                '}';
    }
}
