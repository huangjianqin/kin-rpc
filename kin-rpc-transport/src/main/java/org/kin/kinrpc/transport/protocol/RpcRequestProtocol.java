package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.RpcProtocolId;
import org.kin.transport.netty.socket.protocol.*;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RpcProtocolId.RPC_REQUEST_PROTOCOL_ID)
public class RpcRequestProtocol extends SocketProtocol {
    private byte[] reqContent;

    public static RpcRequestProtocol create(byte[] reqContent) {
        RpcRequestProtocol protocol = ProtocolFactory.createProtocol(RpcProtocolId.RPC_REQUEST_PROTOCOL_ID);
        protocol.reqContent = reqContent;
        return protocol;
    }

    @Override
    public void read(SocketRequestOprs request) {
        reqContent = request.readBytes();
    }

    @Override
    public void write(SocketResponseOprs response) {
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
