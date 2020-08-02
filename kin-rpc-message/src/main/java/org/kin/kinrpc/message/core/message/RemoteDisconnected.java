package org.kin.kinrpc.message.core.message;

import org.kin.kinrpc.transport.domain.RpcAddress;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020/8/2
 */
public class RemoteDisconnected implements Serializable {
    private static final long serialVersionUID = 5344051735870164432L;

    private RpcAddress rpcAddress;

    public static RemoteDisconnected of(RpcAddress rpcAddress) {
        RemoteDisconnected msg = new RemoteDisconnected();
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
