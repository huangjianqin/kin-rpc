package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;

/**
 * message相关常量
 *
 * @author huangjianqin
 * @date 2023/6/8
 */
public final class GrpcMessages {
    private GrpcMessages() {
    }

    /** message专用服务名 */
    public static final String SERVICE_NAME = "$message";

    /** message专用服务方法名 */
    public static final String METHOD_NAME = "requestResponse";

    /** message专用method descriptor */
    public static final MethodDescriptor<ByteBuf, ByteBuf> METHOD_DESCRIPTOR = MethodDescriptor.<ByteBuf, ByteBuf>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(SERVICE_NAME + "/" + METHOD_NAME)
            .setRequestMarshaller(ByteBufMarshaller.DEFAULT)
            .setResponseMarshaller(ByteBufMarshaller.DEFAULT)
            .build();
}
