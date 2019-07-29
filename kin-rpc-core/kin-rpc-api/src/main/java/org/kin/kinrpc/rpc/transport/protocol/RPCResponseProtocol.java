package org.kin.kinrpc.rpc.transport.protocol;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.Protocol;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RPCConstants.RPC_RESPONSE_PROTOCOL_ID)
public class RPCResponseProtocol extends AbstractProtocol {
    private byte[] respContent;

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
