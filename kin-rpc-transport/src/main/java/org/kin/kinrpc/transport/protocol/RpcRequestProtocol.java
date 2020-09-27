package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.RpcProtocolId;
import org.kin.transport.netty.socket.protocol.*;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RpcProtocolId.RPC_REQUEST_PROTOCOL_ID)
public class RpcRequestProtocol extends SocketProtocol {
    /** request唯一id */
    private long requestId;
    /** 序列化类型 */
    private byte serializer;
    /** content */
    private byte[] reqContent;

    public static RpcRequestProtocol create(long requestId, byte serializer, byte[] reqContent) {
        RpcRequestProtocol protocol = ProtocolFactory.createProtocol(RpcProtocolId.RPC_REQUEST_PROTOCOL_ID);
        protocol.requestId = requestId;
        protocol.serializer = serializer;
        protocol.reqContent = reqContent;
        return protocol;
    }

    @Override
    public void read(SocketRequestOprs request) {
        requestId = request.readLong();
        serializer = request.readByte();
        reqContent = request.readBytes();
    }

    @Override
    public void write(SocketResponseOprs response) {
        response.writeLong(requestId);
        response.writeByte(serializer);
        response.writeBytes(reqContent);
    }

    //setter && getter

    public long getRequestId() {
        return requestId;
    }

    public byte getSerializer() {
        return serializer;
    }

    public byte[] getReqContent() {
        return reqContent;
    }
}
