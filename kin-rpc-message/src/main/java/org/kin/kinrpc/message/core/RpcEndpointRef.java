package org.kin.kinrpc.message.core;

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

    private RpcMessage rpcMessage(Serializable message) {
        return new RpcMessage(RpcRequestIdGenerator.next(), rpcEnv.address(), this, message);
    }

    public void send(Serializable message) {
        rpcEnv.send(rpcMessage(message));
    }

    public <R extends Serializable> RpcFuture<R> ask(Serializable message) {
        return rpcEnv.ask(rpcMessage(message));
    }

    //------------------------------------------------------------------------------------------------------------------
    public RpcEndpointAddress getEndpointAddress() {
        return endpointAddress;
    }

    public void setEndpointAddress(RpcEndpointAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }
}
