package org.kin.kinrpc.transport.grpc;

import io.grpc.netty.NettyServerBuilder;
import org.kin.framework.utils.SPI;

/**
 * user自定义{@link NettyServerBuilder}
 * @author huangjianqin
 * @date 2023/6/13
 */
@SPI(alias = "grpcServerCustomizer")
public interface GrpcServerCustomizer {
    /**
     * user自定义grpc server配置
     * @param serverBuilder grpc server builder
     */
    void custom(NettyServerBuilder serverBuilder);
}
