package org.kin.kinrpc.transport.rsocket;

import io.rsocket.core.RSocketConnector;

/**
 * user自定义{@link RSocketConnector}
 * @author huangjianqin
 * @date 2023/6/13
 */
public interface RSocketClientCustomizer {
    /**
     * user自定义rsocket client配置
     * @param server    rsocket client builder
     */
    void custom(RSocketConnector connector);
}
