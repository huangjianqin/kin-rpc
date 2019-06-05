package org.kin.kinrpc.transport.protocol.impl;

import org.kin.kinrpc.transport.protocol.ProtocolHandler;
import org.kin.kinrpc.transport.protocol.Session;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public class PrintProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(Session session, AbstractProtocol protocol) {
        System.out.println(session + "-" + protocol);
    }
}
