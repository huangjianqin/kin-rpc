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
        //更新local RpcEnv
        rpcEnv = RpcEnv.currentRpcEnv();
    }

    public static RpcEndpointRef of(RpcEndpointAddress endpointAddress) {
        RpcEndpointRef rpcEndpointRef = new RpcEndpointRef();
        rpcEndpointRef.endpointAddress = endpointAddress;
        return rpcEndpointRef;
    }

    public static RpcEndpointRef of(RpcEndpointAddress endpointAddress, RpcEnv rpcEnv) {
        RpcEndpointRef rpcEndpointRef = RpcEndpointRef.of(endpointAddress);
        rpcEndpointRef.rpcEnv = rpcEnv;
        return rpcEndpointRef;
    }

    /**
     * 获取rpc环境, 如果实例成员变量没有赋值, 则从ThreadLocal获取
     */
    private RpcEnv rpcEnv() {
        //反序列化时没有获取到RpcEnv, 则尝试从执行线程获取RpcEnv
        if (Objects.isNull(rpcEnv)) {
            synchronized (this) {
                if (Objects.isNull(rpcEnv)) {
                    rpcEnv = RpcEnv.currentRpcEnv();
                }
            }
        }
        return rpcEnv;
    }

    /**
     * 封装成RpcMessage
     */
    private RpcMessage rpcMessage(Serializable message) {
        return RpcMessage.of(RpcRequestIdGenerator.next(), rpcEnv().address(), this, message);
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
