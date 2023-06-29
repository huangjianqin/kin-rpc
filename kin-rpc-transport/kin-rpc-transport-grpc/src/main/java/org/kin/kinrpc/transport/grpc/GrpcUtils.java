package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;

/**
 * @author huangjianqin
 * @date 2023/6/29
 */
public final class GrpcUtils {
    private GrpcUtils() {
    }

    /**
     * 返回grpc method descriptor
     *
     * @param serviceId 服务唯一id
     * @param handlerId 服务方法唯一id
     * @return grpc method descriptor
     */
    public static MethodDescriptor<ByteBuf, ByteBuf> genMethodDescriptor(int serviceId, int handlerId) {
        return MethodDescriptor.<ByteBuf, ByteBuf>newBuilder()
                // TODO: 2023/6/8 如果需要扩展stream request response, 则需要按方法返回值来设置method type
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(GrpcConstants.SERVICE_PREFIX + serviceId + "/" + GrpcConstants.HANDLER_PREFIX + handlerId)
                .setRequestMarshaller(ByteBufMarshaller.DEFAULT)
                .setResponseMarshaller(ByteBufMarshaller.DEFAULT)
                .build();
    }
}
