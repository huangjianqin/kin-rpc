package org.kin.kinrpc.transport.kinrpc;

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

    public RpcRequest() {

    }

    public RpcRequest(long requestId, String serviceKey, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceKey = serviceKey;
        this.method = method;
        this.params = params;
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

    @Override
    public String toString() {
        return "RpcRequest{" +
                "serviceKey='" + serviceKey + '\'' +
                ", method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                ", requestId=" + requestId +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                '}';
    }
}
