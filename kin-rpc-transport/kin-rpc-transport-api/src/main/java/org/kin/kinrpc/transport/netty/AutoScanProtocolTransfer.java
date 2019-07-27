package org.kin.kinrpc.transport.netty;

import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;

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
