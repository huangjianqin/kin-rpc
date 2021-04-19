package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RpcRequest extends AbstractRpcMessage implements Serializable {
    private static final long serialVersionUID = 5417022481782277610L;

    /** 请求参数 */
    private String serviceKey;
    private String method;
    private Object[] params;
    /** 用于provider直接拒绝call timeout请求, 而不用处理request */
    private int callTimeout;

    public RpcRequest() {

    }

    public RpcRequest(long requestId, Url url, String serviceKey, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceKey = serviceKey;
        this.method = method;
        this.params = params;
        this.callTimeout = url.getIntParam(Constants.CALL_TIMEOUT_KEY);
    }

    /**
     * @return 此请求是否call timeout
     */
    public boolean isCallTimeout() {
        return createTime + callTimeout < System.currentTimeMillis();
    }

    //setter && getter
    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
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

    public int getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(int callTimeout) {
        this.callTimeout = callTimeout;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId=" + requestId +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                ", serviceKey='" + serviceKey + '\'' +
                ", method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                ", callTimeout=" + callTimeout +
                '}';
    }
}
