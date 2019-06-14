package org.kin.kinrpc.rpc.transport;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.domain.Request;
import org.kin.kinrpc.transport.protocol.domain.Response;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
public class RPCResponseProtocol extends AbstractProtocol {
    private byte[] respContent;

    public RPCResponseProtocol(int protocolId) {
        super(protocolId);
        Preconditions.checkArgument(protocolId == RPCConstants.RPC_RESPONSE_PROTOCOL_ID);
    }

    public RPCResponseProtocol(byte[] respContent) {
        super(RPCConstants.RPC_RESPONSE_PROTOCOL_ID);
        this.respContent = respContent;
    }

    @Override
    public void read(Request request) {
        respContent = request.readBytes();
    }

    @Override
    public void write(Response response) {
        response.writeBytes(respContent);
    }

    //setter && getter
    public byte[] getRespContent() {
        return respContent;
    }

    public void setRespContent(byte[] respContent) {
        this.respContent = respContent;
    }
}
