package org.kin.kinrpc.transport.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.internal.GrpcAttributes;
import org.kin.kinrpc.transport.grpc.GrpcServerCallContext;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class DefaultServerInterceptor implements ServerInterceptor {
    /** 单例 */
    public static final DefaultServerInterceptor INSTANCE = new DefaultServerInterceptor();

    private DefaultServerInterceptor() {
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        try {
            GrpcServerCallContext.initLocal(call.getAttributes(), headers);
            return next.startCall(call, headers);
        }finally {
            GrpcServerCallContext.clearLocal();
        }
    }
}
