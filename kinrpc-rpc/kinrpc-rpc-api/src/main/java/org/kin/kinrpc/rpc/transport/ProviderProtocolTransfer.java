package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.protocol.RPCHeartbeat;
import org.kin.kinrpc.rpc.transport.protocol.RPCRequestProtocol;
import org.kin.kinrpc.transport.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class ProviderProtocolTransfer implements Bytes2ProtocolTransfer {
    @Override
    public AbstractProtocol transfer(Request request) {
        AbstractProtocol protocol;
        if(request.getProtocolId() == RPCConstants.RPC_REQUEST_PROTOCOL_ID){
            protocol = new RPCRequestProtocol(request.getProtocolId());
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
