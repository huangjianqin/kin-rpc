package org.kin.kinrpc;

import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.utils.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
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
        this.serverAttachments = CollectionUtils.isNonEmpty(serverAttachments) ? serverAttachments : Collections.emptyMap();
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
    public int handlerId() {
        return methodMetadata.handlerId();
    }

    @Override
    public String handler() {
        return methodMetadata.handler();
    }

    @Override
    public String getHandlerName() {
        return methodMetadata.handlerName();
    }

    @Override
    public Object[] params() {
        return params;
    }

    @Override
    public boolean isAsyncReturn() {
        return methodMetadata.isAsyncReturn();
    }

    @Override
    public boolean isOneWay() {
        return methodMetadata.isOneWay();
    }

    @Override
    public Class<?> realReturnType() {
        return methodMetadata.realReturnType();
    }

    @Override
    public Class<?> returnType() {
        return methodMetadata.returnType();
    }

    @Override
    public boolean isObjectMethod() {
        return methodMetadata.isObjectMethod();
    }

    @Override
    public Map<String, String> getServerAttachments() {
        return serverAttachments;
    }

    //getter
    public MethodMetadata getMethodMetadata() {
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
