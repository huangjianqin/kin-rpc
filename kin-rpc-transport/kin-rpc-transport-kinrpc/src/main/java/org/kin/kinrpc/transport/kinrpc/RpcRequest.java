package org.kin.kinrpc.transport.kinrpc;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RpcRequest extends AbstractRpcMessage implements Serializable {
    private static final long serialVersionUID = 5417022481782277610L;

    /** 请求参数 */
    private String serviceName;
    private String method;
    private Object[] params;

    public RpcRequest() {

    }

    public RpcRequest(long requestId, String serviceName, String method, Object[] params) {
        this.requestId = requestId;
        this.serviceName = serviceName;
        this.method = method;
        this.params = params;
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

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "serviceName='" + serviceName + '\'' +
                ", method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                ", requestId=" + requestId +
                ", createTime=" + createTime +
                ", eventTime=" + eventTime +
                ", handleTime=" + handleTime +
                '}';
    }
}
