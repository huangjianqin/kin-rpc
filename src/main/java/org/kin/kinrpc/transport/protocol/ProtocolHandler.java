package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/5/30.
 */
@FunctionalInterface
public interface ProtocolHandler {
    /**
     * 在channel线程调用
     */
    void handleProtocol(Session session, AbstractProtocol protocol);
}
