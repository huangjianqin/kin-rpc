package org.kin.kinrpc.rpc.transport.domain;

import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RPCResponse implements Serializable {
    private int requestId;
    //统计日志打印用
    private String serviceName;
    private String method;

    private Object result;
    private String info;
    private State state;

    public enum State {
        /**
         * 服务调用错误
         */
        ERROR(-1),
        /**
         * 服务调用成功
         */
        SUCCESS(1),
        /**
         * 告诉消费者请求其他server的该服务调用
         */
        RETRY(0),
        ;

        int code;

        State(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    //request创建时间
    private long createTime;
    //request事件时间即, 到达service端的时间
    private long eventTime;
    //request处理时间
    private long handleTime;

    public RPCResponse() {
    }

    public RPCResponse(int requestId, String serviceName, String method) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
    }

    private static RPCResponse respWith(int requestId, String serviceName, String method) {
        RPCResponse rpcResponse = new RPCResponse(requestId, serviceName, method);
        return rpcResponse;
    }

    public static RPCResponse respWithError(int requestId, String serviceName, String method, String errorMsg) {
        RPCResponse rpcResponse = respWith(requestId, serviceName, method);
        rpcResponse.setState(RPCResponse.State.ERROR, errorMsg);
        return rpcResponse;
    }

    public static RPCResponse respWithError(RPCRequest request, String errorMsg) {
        RPCResponse rpcResponse = respWithError(request.getRequestId(), request.getServiceName(), request.getMethod(), errorMsg);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        return rpcResponse;
    }

    public static RPCResponse respWithRetry(int requestId, String serviceName, String method, String retryMsg) {
        RPCResponse rpcResponse = respWith(requestId, serviceName, method);
        rpcResponse.setState(State.RETRY, retryMsg);
        return rpcResponse;
    }

    public static RPCResponse respWithRetry(RPCRequest request, String errorMsg) {
        RPCResponse rpcResponse = respWithRetry(request.getRequestId(), request.getServiceName(), request.getMethod(), errorMsg);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        return rpcResponse;
    }

    public void setState(State state, String info) {
        this.state = state;
        this.info = info;
    }

    //setter && getter
    public State getState() {
        return state;
    }

    public String getInfo() {
        return info;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethod() {
        return method;
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
        if (!(o instanceof RPCResponse)) {
            return false;
        }

        RPCResponse that = (RPCResponse) o;

        return requestId == that.requestId;

    }

    @Override
    public int hashCode() {
        return requestId;
    }

    @Override
    public String toString() {
        return "RPCResponse{" +
                "requestId=" + requestId +
                ", serviceName='" + serviceName + '\'' +
                ", method='" + method + '\'' +
                ", result=" + result +
                ", info='" + info + '\'' +
                ", state=" + state +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                '}';
    }
}
