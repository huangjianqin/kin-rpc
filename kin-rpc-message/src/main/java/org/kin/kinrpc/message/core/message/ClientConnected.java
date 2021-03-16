package org.kin.kinrpc.message.core.message;

import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020/8/2
 */
public final class ClientConnected implements Serializable {
    private static final long serialVersionUID = -2958636572550283276L;

    private KinRpcAddress rpcAddress;

    public static ClientConnected of(KinRpcAddress rpcAddress) {
        ClientConnected msg = new ClientConnected();
        msg.rpcAddress = rpcAddress;
        return msg;
    }

    public KinRpcAddress getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(KinRpcAddress rpcAddress) {
        this.rpcAddress = rpcAddress;
    }
}

