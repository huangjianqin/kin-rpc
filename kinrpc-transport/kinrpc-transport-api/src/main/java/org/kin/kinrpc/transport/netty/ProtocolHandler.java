package org.kin.kinrpc.transport.netty;

import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/5/30.
 */
@FunctionalInterface
public interface ProtocolHandler<T extends AbstractProtocol> {
    /**
     * 在channel线程调用, 最好内部捕获异常, 不然会导致channel因异常关闭
     */
    void handleProtocol(AbstractSession session, T protocol);
}
