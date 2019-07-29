package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public class PrintProtocolHandler implements ProtocolHandler {
    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        System.out.println(session + "-" + protocol);
    }
}
