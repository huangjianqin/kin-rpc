package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.RpcProtocolId;
import org.kin.transport.netty.socket.protocol.*;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RpcProtocolId.RPC_RESPONSE_PROTOCOL_ID)
public class RpcResponseProtocol extends SocketProtocol {
    /** request唯一id */
    private long requestId;
    /** 序列化类型 */
    private byte serializer;
    /** content */
    private byte[] respContent;

    public static RpcResponseProtocol create(long requestId, byte serializer, byte[] respContent) {
        RpcResponseProtocol protocol = ProtocolFactory.createProtocol(RpcProtocolId.RPC_RESPONSE_PROTOCOL_ID);
        protocol.requestId = requestId;
        protocol.serializer = serializer;
        protocol.respContent = respContent;
        return protocol;
    }

    @Override
    public void read(SocketRequestOprs request) {
        requestId = request.readLong();
        serializer = request.readByte();
        respContent = request.readBytes();
    }

    @Override
    public void write(SocketResponseOprs response) {
        response.writeLong(requestId);
        response.writeByte(serializer);
        response.writeBytes(respContent);
    }

    //setter && getter
    public long getRequestId() {
        return requestId;
    }

    public byte getSerializer() {
        return serializer;
    }

    public byte[] getRespContent() {
        return respContent;
    }
}
