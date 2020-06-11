package org.kin.kinrpc.message.api;

import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.domain.RpcRequestIdGenerator;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcEndpointRef implements Serializable {
    private static final long serialVersionUID = 3191956547695414179L;

    private RpcEndpointAddress endpointAddress;
    private transient volatile RpcEnv rpcEnv;

    public RpcEndpointRef(RpcEndpointAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public void updateRpcEnv(RpcEnv rpcEnv) {
        this.rpcEnv = rpcEnv;
    }

    public void send(RpcEndpointRef replier, Serializable message) {
        rpcEnv.send(new RpcMessage(RpcRequestIdGenerator.next(), replier, this, message));
    }

    //------------------------------------------------------------------------------------------------------------------
    public RpcEndpointAddress getEndpointAddress() {
        return endpointAddress;
    }

    public void setEndpointAddress(RpcEndpointAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }
}
