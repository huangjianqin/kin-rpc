package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public class DefaultProtocolTransfer implements Bytes2ProtocolTransfer {
    private static final DefaultProtocolTransfer INSTANCE = new DefaultProtocolTransfer();

    private DefaultProtocolTransfer() {
    }

    public static DefaultProtocolTransfer instance() {
        return INSTANCE;
    }

    @Override
    public <T extends AbstractProtocol> T transfer(Request request) {
        AbstractProtocol protocol = ProtocolFactory.createProtocol(request.getProtocolId());
        protocol.read(request);
        return (T) protocol;
    }
}
