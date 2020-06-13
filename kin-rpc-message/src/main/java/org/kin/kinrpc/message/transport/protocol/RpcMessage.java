package org.kin.kinrpc.message.transport.protocol;

import org.kin.kinrpc.message.core.RpcEndpointRef;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcMessage implements Serializable {
    private static final long serialVersionUID = -7580281019273609173L;

    /** 请求唯一id */
    private long requestId;
    /** 发送者 */
    private RpcEndpointRef from;
    /** 接收方 */
    private RpcEndpointRef to;
    /** 消息 */
    private Serializable message;
    /** request创建时间 */
    private long createTime;

    public RpcMessage() {
    }

    public RpcMessage(long requestId, RpcEndpointRef from, RpcEndpointRef to, Serializable message) {
        this.requestId = requestId;
        this.from = from;
        this.to = to;
        this.message = message;
        this.createTime = System.currentTimeMillis();
    }

    //----------------------------------------------------------------------------------------------------------------

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public RpcEndpointRef getFrom() {
        return from;
    }

    public void setFrom(RpcEndpointRef from) {
        this.from = from;
    }

    public RpcEndpointRef getTo() {
        return to;
    }

    public void setTo(RpcEndpointRef to) {
        this.to = to;
    }

    public Serializable getMessage() {
        return message;
    }

    public void setMessage(Serializable message) {
        this.message = message;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
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


}
