package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.HandlerUtils;

/**
 * message相关常量
 *
 * @author huangjianqin
 * @date 2023/6/8
 */
public final class GrpcMessages {
    /** message专用服务名 */
    public static final String SERVICE_NAME = "$message";

    /** message专用服务方法名 */
    public static final String METHOD_NAME = "requestResponse";

    /** message专用method descriptor */
    public static final MethodDescriptor<ByteBuf, ByteBuf> METHOD_DESCRIPTOR =
            GrpcUtils.genMethodDescriptor(GsvUtils.serviceId(SERVICE_NAME),
                    HandlerUtils.handlerId(SERVICE_NAME, METHOD_NAME));

    private GrpcMessages() {
    }
}
