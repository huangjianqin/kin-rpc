package org.kin.kinrpc.transport.protocol;

import java.io.Serializable;
import java.util.Objects;

/**
 * RPC请求响应的共用参数
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class AbstractRpcMessage implements Serializable {
    private static final long serialVersionUID = 2498552297322187595L;

    /** 唯一id */
    protected long requestId;
    /** request创建时间 */
    protected long createTime;
    /** request事件时间即, 到达service端的时间 */
    protected long eventTime;
    /** request处理时间 */
    protected long handleTime;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public long getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(long handleTime) {
        this.handleTime = handleTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRpcMessage that = (AbstractRpcMessage) o;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "AbstractRpcProtocol{" +
                "requestId=" + requestId +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                '}';
    }
}
