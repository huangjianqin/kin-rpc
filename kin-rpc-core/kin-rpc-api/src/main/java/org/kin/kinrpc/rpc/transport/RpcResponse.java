package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.transport.protocol.AbstractRpcMessage;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RpcResponse extends AbstractRpcMessage implements Serializable {
    private static final long serialVersionUID = -7580386808779240788L;

    /** 统计日志打印用 */
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

    public RpcResponse() {
    }

    public RpcResponse(long requestId, String serviceName, String method) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
    }

    private static RpcResponse respWith(long requestId, String serviceName, String method) {
        return new RpcResponse(requestId, serviceName, method);
    }

    public static RpcResponse respWithError(long requestId, String errorMsg) {
        RpcResponse rpcResponse = respWith(requestId, "", "");
        rpcResponse.setState(RpcResponse.State.ERROR, errorMsg);
        return rpcResponse;
    }

    public static RpcResponse respWithError(long requestId, String serviceName, String method, String errorMsg) {
        RpcResponse rpcResponse = respWith(requestId, serviceName, method);
        rpcResponse.setState(RpcResponse.State.ERROR, errorMsg);
        return rpcResponse;
    }

    public static RpcResponse respWithError(RpcRequest request, String errorMsg) {
        RpcResponse rpcResponse = respWithError(request.getRequestId(), request.getServiceName(), request.getMethod(), errorMsg);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        return rpcResponse;
    }

    public static RpcResponse respWithRetry(long requestId, String serviceName, String method, String retryMsg) {
        RpcResponse rpcResponse = respWith(requestId, serviceName, method);
        rpcResponse.setState(State.RETRY, retryMsg);
        return rpcResponse;
    }

    public static RpcResponse respWithRetry(RpcRequest request, String errorMsg) {
        RpcResponse rpcResponse = respWithRetry(request.getRequestId(), request.getServiceName(), request.getMethod(), errorMsg);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        return rpcResponse;
    }

    public void setState(State state, String info) {
        this.state = state;
        this.info = info;
    }

    //setter && getter

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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcResponse that = (RpcResponse) o;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
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
