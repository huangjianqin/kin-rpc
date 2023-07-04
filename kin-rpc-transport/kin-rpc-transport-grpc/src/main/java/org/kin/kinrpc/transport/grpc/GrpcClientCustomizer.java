package org.kin.kinrpc.transport.grpc;

import io.grpc.netty.NettyChannelBuilder;
import org.kin.framework.utils.SPI;

/**
 * user自定义{@link NettyChannelBuilder}
 * @author huangjianqin
 * @date 2023/6/13
 */
@SPI(alias = "grpcClientCustomizer")
public interface GrpcClientCustomizer {
    /**
     * user自定义grpc channel配置
     * @param channelBuilder    grpc channel builder
     */
    void custom(NettyChannelBuilder channelBuilder);
}
