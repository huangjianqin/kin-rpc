package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.transport.protocol.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.protocol.domain.Request;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCResponseProtocolTransfer implements Bytes2ProtocolTransfer {
    @Override
    public RPCResponseProtocol transfer(Request request) {
        RPCResponseProtocol protocol = new RPCResponseProtocol(request.getProtocolId());
        protocol.read(request);
        return protocol;
    }
}
