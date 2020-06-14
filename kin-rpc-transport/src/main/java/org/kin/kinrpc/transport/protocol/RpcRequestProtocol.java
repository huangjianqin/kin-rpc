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
@Protocol(id = RpcProtocolId.RPC_REQUEST_PROTOCOL_ID)
public class RpcRequestProtocol extends AbstractProtocol {
    private byte[] reqContent;

    public static RpcRequestProtocol create(byte[] reqContent) {
        RpcRequestProtocol protocol = ProtocolFactory.createProtocol(RpcProtocolId.RPC_REQUEST_PROTOCOL_ID);
        protocol.reqContent = reqContent;
        return protocol;
    }

    @Override
    public void read(Request request) {
        reqContent = request.readBytes();
    }

    @Override
    public void write(Response response) {
        response.writeBytes(reqContent);
    }

    //setter && getter

    public byte[] getReqContent() {
        return reqContent;
    }

    public void setReqContent(byte[] reqContent) {
        this.reqContent = reqContent;
    }
}