package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public class AutoScanProtocolTransfer implements Bytes2ProtocolTransfer {
    private ProtocolFactory protocolFactory;

    public AutoScanProtocolTransfer(String scanPath) {

    }

    @Override
    public <T extends AbstractProtocol> T transfer(Request request) {
        return null;
    }
}
