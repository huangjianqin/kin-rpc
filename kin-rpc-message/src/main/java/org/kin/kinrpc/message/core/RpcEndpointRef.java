package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.domain.RpcRequestIdGenerator;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcEndpointRef implements Serializable {
    private static final long serialVersionUID = 3191956547695414179L;
    /** remote address */
    private RpcEndpointAddress endpointAddress;
    /** rpc环境 */
    private transient volatile RpcEnv rpcEnv;

    public RpcEndpointRef() {
    }

    public RpcEndpointRef(RpcEndpointAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public RpcEndpointRef(RpcEndpointAddress endpointAddress, RpcEnv rpcEnv) {
        this.endpointAddress = endpointAddress;
        this.rpcEnv = rpcEnv;
    }

    /**
     * 获取rpc环境, 如果实例成员变量没有赋值, 则从ThreadLocal获取
     */
    private RpcEnv rpcEnv() {
        RpcEnv rpcEnv = this.rpcEnv;
        if (Objects.isNull(rpcEnv)) {
            rpcEnv = RpcEnv.currentRpcEnv();
        }
        return rpcEnv;
    }

    private RpcMessage rpcMessage(Serializable message) {
        return new RpcMessage(RpcRequestIdGenerator.next(), rpcEnv().address(), this, message);
    }

    /**
     * 发送消息
     */
    public void send(Serializable message) {
        rpcEnv().send(rpcMessage(message));
    }

    /**
     * 发送消息,并返回Future, 支持阻塞等待待消息处理完并返回
     */
    public <R extends Serializable> RpcFuture<R> ask(Serializable message) {
        return rpcEnv().ask(rpcMessage(message));
    }

    //------------------------------------------------------------------------------------------------------------------
    public RpcEndpointAddress getEndpointAddress() {
        return endpointAddress;
    }

    public void setEndpointAddress(RpcEndpointAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }
}
