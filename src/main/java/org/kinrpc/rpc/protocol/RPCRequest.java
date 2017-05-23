package org.kinrpc.rpc.protocol;

import io.netty.channel.ChannelHandlerContext;

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
    //返回RPCResponse的ChannelHandlerContext
    transient private ChannelHandlerContext ctx;

    public RPCRequest(){

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

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
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

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
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
