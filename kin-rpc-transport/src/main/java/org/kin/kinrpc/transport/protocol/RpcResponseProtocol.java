package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.RpcProtocolId;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.kin.transport.netty.core.protocol.Protocol;
import org.kin.transport.netty.core.protocol.ProtocolFactory;
import org.kin.transport.netty.core.protocol.domain.Request;
import org.kin.transport.netty.core.protocol.domain.Response;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RpcProtocolId.RPC_RESPONSE_PROTOCOL_ID)
public class RpcResponseProtocol extends AbstractProtocol {
    private byte[] respContent;

    public static RpcResponseProtocol create(byte[] respContent) {
        RpcResponseProtocol protocol = ProtocolFactory.createProtocol(RpcProtocolId.RPC_RESPONSE_PROTOCOL_ID);
        protocol.respContent = respContent;
        return protocol;
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
