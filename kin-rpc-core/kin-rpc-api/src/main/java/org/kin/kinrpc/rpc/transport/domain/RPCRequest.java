package org.kin.kinrpc.rpc.transport.domain;

import io.netty.channel.Channel;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RPCRequest implements Serializable {
    private String requestId;
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

    public RPCRequest() {

    }

    public RPCRequest(String requestId, String serviceName, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
        this.params = params;
    }

    //setter && getter

    public String getRequestId() {
        return requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParams() {
        return params;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof RPCRequest)) {
            return false;
        }

        RPCRequest request = (RPCRequest) o;

        return requestId == request.requestId;

    }

    @Override
    public int hashCode() {
        return requestId.hashCode();
    }

    @Override
    public String toString() {
        return "RPCRequest{" +
                "requestId=" + requestId +
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
