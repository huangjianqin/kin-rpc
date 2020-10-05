package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.RpcProtocolId;
import org.kin.transport.netty.socket.protocol.Protocol;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;
import org.kin.transport.netty.socket.protocol.SocketProtocol;

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

    //setter && getter
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte getSerializer() {
        return serializer;
    }

    public void setSerializer(byte serializer) {
        this.serializer = serializer;
    }

    public byte[] getReqContent() {
        return reqContent;
    }

    public void setReqContent(byte[] reqContent) {
        this.reqContent = reqContent;
    }
}
