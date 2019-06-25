package org.kin.kinrpc.transport.protocol.impl;

import org.kin.kinrpc.transport.protocol.ProtocolHandler;
import org.kin.kinrpc.transport.protocol.AbstractSession;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public class PrintProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        System.out.println(session + "-" + protocol);
    }
}
