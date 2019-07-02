package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.domain.RPCHeartbeat;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestProtocol;
import org.kin.kinrpc.rpc.transport.domain.RPCResponseProtocol;
import org.kin.kinrpc.transport.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.domain.AbstractProtocol;
import org.kin.kinrpc.transport.domain.Request;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class ReferenceProtocolTransfer implements Bytes2ProtocolTransfer {
    @Override
    public AbstractProtocol transfer(Request request) {
        AbstractProtocol protocol;
        if(request.getProtocolId() == RPCConstants.RPC_RESPONSE_PROTOCOL_ID){
            protocol = new RPCResponseProtocol(request.getProtocolId());
        }
        else if(request.getProtocolId() == RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID){
            protocol = new RPCHeartbeat(request.getProtocolId());
        }
        else{
            return null;
        }
        protocol.read(request);
        return protocol;
    }
}
