package org.kin.kinrpc.transport.rpc.domain;

import io.netty.channel.Channel;

import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RPCRequest implements Serializable {
    private int requestId;
    //请求参数
    private String serviceName;
    private String method;
    private Object[] params;
    //请求的channel
    transient private Channel channel;

    public RPCRequest() {

    }

    public RPCRequest(int requestId) {
        this.requestId = requestId;
    }

    public RPCRequest(int requestId, String serviceName, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
        this.params = params;
    }

    //setter && getter
    public int getRequestId() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RPCRequest)) return false;

        RPCRequest request = (RPCRequest) o;

        return requestId == request.requestId;

    }

    @Override
    public int hashCode() {
        return requestId;
    }

    @Override
    public String toString() {
        return "RPCRequest{" +
                "requestId=" + requestId +
                ", serviceName='" + serviceName + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
