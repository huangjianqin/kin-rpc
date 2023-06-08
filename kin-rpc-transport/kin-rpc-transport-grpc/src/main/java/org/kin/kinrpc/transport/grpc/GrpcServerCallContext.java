package org.kin.kinrpc.transport.grpc;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.ServerCall;

import javax.annotation.Nullable;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class GrpcServerCallContext {
    private static final ThreadLocal<GrpcServerCallContext> THREAD_LOCAL = new ThreadLocal<>();

    /** {@link ServerCall#getAttributes()} */
    private final Attributes attributes;
    /** server call headers */
    private final Metadata headers;

    /**
     * 获取当前thread local grpc server call context
     * @return  {@link GrpcServerCallContext}实例
     */
    @Nullable
    public static GrpcServerCallContext current(){
        return THREAD_LOCAL.get();
    }

    /**
     * 初始化thread local grpc server call context
     * @param attributes    grpc server call attributes
     * @param headers   grpc server call headers
     */
    public static void initLocal(Attributes attributes, Metadata headers){
        THREAD_LOCAL.set(new GrpcServerCallContext(attributes, headers));
    }

    /**
     * 清掉thread local grpc server call context
     */
    public static void clearLocal(){
        THREAD_LOCAL.set(null);
    }

    private GrpcServerCallContext(Attributes attributes, Metadata headers) {
        this.attributes = attributes;
        this.headers = headers;
    }

    //getter
    public Attributes attributes() {
        return attributes;
    }

    public Metadata headers() {
        return headers;
    }
}
