package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.HandlerUtils;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public final class GrpcConstants {
    /** 服务前缀 */
    public static final String SERVICE_PREFIX = "s";
    /** 服务方法前缀 */
    public static final String HANDLER_PREFIX = "h";

    /** generic服务名 */
    public static final String GENERIC_SERVICE_NAME = "$generic";

    /** generic专用服务方法名 */
    public static final String GENERIC_METHOD_NAME = "requestResponse";

    /** generic专用method descriptor */
    public static final MethodDescriptor<ByteBuf, ByteBuf> GENERIC_METHOD_DESCRIPTOR =
            GrpcUtils.genMethodDescriptor(GsvUtils.serviceId(GENERIC_SERVICE_NAME),
                    HandlerUtils.handlerId(GENERIC_SERVICE_NAME, GENERIC_METHOD_NAME));

    private GrpcConstants() {
    }
}
