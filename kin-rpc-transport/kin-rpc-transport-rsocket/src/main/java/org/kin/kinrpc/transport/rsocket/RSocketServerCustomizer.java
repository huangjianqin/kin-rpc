package org.kin.kinrpc.transport.rsocket;

import org.kin.framework.utils.SPI;

/**
 * user自定义{@link io.rsocket.core.RSocketServer}
 *
 * @author huangjianqin
 * @date 2023/6/13
 */
@SPI("rsocketServerCustomizer")
public interface RSocketServerCustomizer {
    /**
     * user自定义rsocket server配置
     * @param server    rsocket server builder
     */
    void custom(io.rsocket.core.RSocketServer server);
}
