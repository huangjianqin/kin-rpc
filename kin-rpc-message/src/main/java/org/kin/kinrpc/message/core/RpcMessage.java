package org.kin.kinrpc.message.core;

import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;

import java.io.Serializable;
import java.util.Objects;

/**
 * 包装rpc请求的消息
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
final class RpcMessage implements Serializable {
    private static final long serialVersionUID = -7580281019273609173L;

    /** 请求唯一id */
    private long requestId;
    /** 发送者地址 */
    private KinRpcAddress fromAddress;
    /** 接收方 */
    private RpcEndpointRef to;
    /** 消息 */
    private Serializable message;
    /** 消息创建时间 */
    private long createTime;

    static RpcMessage of(long requestId, KinRpcAddress fromAddress, RpcEndpointRef to, Serializable message) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.requestId = requestId;
        rpcMessage.fromAddress = fromAddress;
        rpcMessage.to = to;
        rpcMessage.message = message;
        rpcMessage.createTime = System.currentTimeMillis();
        return rpcMessage;
    }

    //----------------------------------------------------------------------------------------------------------------
    long getRequestId() {
        return requestId;
    }

    void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    KinRpcAddress getFromAddress() {
        return fromAddress;
    }

    void setFromAddress(KinRpcAddress fromAddress) {
        this.fromAddress = fromAddress;
    }

    RpcEndpointRef getTo() {
        return to;
    }

    void setTo(RpcEndpointRef to) {
        this.to = to;
    }

    Serializable getMessage() {
        return message;
    }

    void setMessage(Serializable message) {
        this.message = message;
    }

    long getCreateTime() {
        return createTime;
    }

    void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcMessage that = (RpcMessage) o;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "RpcMessage{" +
                "requestId=" + requestId +
                ", fromAddress=" + fromAddress +
                ", to=" + to +
                ", message=" + message +
                ", createTime=" + createTime +
                '}';
    }
}
