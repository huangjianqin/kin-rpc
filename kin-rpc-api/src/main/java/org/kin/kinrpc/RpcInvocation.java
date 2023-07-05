package org.kin.kinrpc;

import org.kin.framework.collection.AttachmentMap;

import java.util.Arrays;

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
    /** 服务方法元数据 */
    private final MethodMetadata methodMetadata;
    /** 序列化类型code */
    private final byte serializationCode;

    public RpcInvocation(int serviceId,
                         String service,
                         Object[] params,
                         MethodMetadata methodMetadata,
                         byte serializationCode) {
        this.serviceId = serviceId;
        this.service = service;
        this.params = params;
        this.methodMetadata = methodMetadata;
        this.serializationCode = serializationCode;
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
    public byte serializationCode() {
        return serializationCode;
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
                ", methodMetadata=" + methodMetadata +
                ", serializationCode=" + serializationCode +
                ", attachments=" + attachments() +
                '}';
    }
}
