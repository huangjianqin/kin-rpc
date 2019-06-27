package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.transport.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.domain.Request;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCRequestProtocolTransfer implements Bytes2ProtocolTransfer {
    @Override
    public RPCRequestProtocol transfer(Request request) {
        RPCRequestProtocol protocol = new RPCRequestProtocol(request.getProtocolId());
        protocol.read(request);
        return protocol;
    }
}
