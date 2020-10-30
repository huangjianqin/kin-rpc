package org.kin.kinrpc.message.core.message;

import org.kin.kinrpc.transport.domain.RpcAddress;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020/8/2
 */
public class ClientConnected implements Serializable {
    private static final long serialVersionUID = -2958636572550283276L;

    private RpcAddress rpcAddress;

    public static ClientConnected of(RpcAddress rpcAddress) {
        ClientConnected msg = new ClientConnected();
        msg.rpcAddress = rpcAddress;
        return msg;
    }

    public RpcAddress getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(RpcAddress rpcAddress) {
        this.rpcAddress = rpcAddress;
    }
}

