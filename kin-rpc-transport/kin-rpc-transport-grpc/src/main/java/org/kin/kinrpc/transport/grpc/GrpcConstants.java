package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public final class GrpcConstants {
    private GrpcConstants() {
    }

    /** generic服务名 */
    public static final String GENERIC_SERVICE_NAME = "$generic";

    /** generic专用服务方法名 */
    public static final String GENERIC_METHOD_NAME = "requestResponse";

    /** generic专用method descriptor */
    public static final MethodDescriptor<ByteBuf, ByteBuf> GENERIC_METHOD_DESCRIPTOR = MethodDescriptor.<ByteBuf, ByteBuf>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(GENERIC_SERVICE_NAME + "/" + GENERIC_METHOD_NAME)
            .setRequestMarshaller(ByteBufMarshaller.DEFAULT)
            .setResponseMarshaller(ByteBufMarshaller.DEFAULT)
            .build();
}
