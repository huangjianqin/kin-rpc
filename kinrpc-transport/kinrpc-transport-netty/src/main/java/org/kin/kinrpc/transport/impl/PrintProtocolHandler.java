package org.kin.kinrpc.transport.impl;

import org.kin.kinrpc.transport.AbstractSession;
import org.kin.kinrpc.transport.ProtocolHandler;
import org.kin.kinrpc.transport.domain.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public class PrintProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        System.out.println(session + "-" + protocol);
    }
}
