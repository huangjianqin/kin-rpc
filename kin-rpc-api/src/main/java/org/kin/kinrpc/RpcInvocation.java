package org.kin.kinrpc;

import org.kin.framework.collection.AttachmentMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务调用上下文信息
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class RpcInvocation extends AttachmentMap implements Invocation {
    /** 服务唯一id */
    private final int serviceId;
    /** 服务唯一标识 */
    private final String service;
    /** 方法参数实例 */
    private final Object[] params;
    /** 发送给server的attachments */
    private final Map<String, String> serverAttachments;
    /** 服务方法元数据 */
    private final MethodMetadata methodMetadata;

    public RpcInvocation(int serviceId,
                         String service,
                         Object[] params,
                         Map<String, String> serverAttachments,
                         MethodMetadata methodMetadata) {
        this.serviceId = serviceId;
        this.service = service;
        this.params = params;
        this.serverAttachments = new HashMap<>(serverAttachments);
        this.methodMetadata = methodMetadata;
    }

    @Override
    public int serviceId() {
        return serviceId;
    }

    @Override
    public String service() {
        return service;
    }

    @Override
    public Object[] params() {
        return params;
    }

    @Override
    public Map<String, String> serverAttachments() {
        return serverAttachments;
    }

    @Override
    public MethodMetadata methodMetadata() {
        return methodMetadata;
    }

    @Override
    public String toString() {
        return "RpcInvocation{" +
                "serviceId=" + serviceId +
                ", service='" + service + '\'' +
                ", params=" + Arrays.deepToString(params) +
                ", serverAttachments=" + serverAttachments +
                ", methodMetadata=" + methodMetadata +
                ", attachments=" + attachments() +
                '}';
    }
}
