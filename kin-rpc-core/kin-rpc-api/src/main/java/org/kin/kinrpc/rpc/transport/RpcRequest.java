package org.kin.kinrpc.rpc.transport;

import io.netty.channel.Channel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 5417022481782277610L;

    private long requestId;
    /** 请求参数 */
    private String serviceName;
    private String method;
    private Object[] params;
    /** request创建时间 */
    private long createTime;
    /** request事件时间即, 到达service端的时间 */
    private long eventTime;
    /** request处理时间 */
    private long handleTime;

    /** 请求的channel */
    transient private Channel channel;

    public RpcRequest() {

    }

    public RpcRequest(long requestId, String serviceName, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
        this.params = params;
    }

    //setter && getter
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
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

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcRequest that = (RpcRequest) o;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                ", channel=" + channel +
                '}';
    }
}
