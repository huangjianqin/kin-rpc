package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.transport.netty.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.netty.ProtocolFactory;
import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class ProviderProtocolTransfer implements Bytes2ProtocolTransfer {
    @Override
    public AbstractProtocol transfer(Request request) {
        AbstractProtocol protocol = ProtocolFactory.createProtocol(request.getProtocolId());
        protocol.read(request);
        return protocol;
    }
}
