package org.kin.kinrpc.transport.kinrpc;

import org.kin.transport.netty.socket.protocol.Protocol;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;
import org.kin.transport.netty.socket.protocol.SocketProtocol;

/**
 * 相对server而言, 请求协议
 * Created by huangjianqin on 2019/6/14.
 */
@Protocol(id = KinRpcProtocolId.RPC_REQUEST_PROTOCOL_ID)
public class KinRpcRequestProtocol extends SocketProtocol {
    /** request唯一id */
    private long requestId;
    /** 序列化类型 */
    private byte serializer;
    /** content */
    private byte[] reqContent;

    public static KinRpcRequestProtocol create(long requestId, byte serializer, byte[] reqContent) {
        KinRpcRequestProtocol protocol = ProtocolFactory.createProtocol(KinRpcProtocolId.RPC_REQUEST_PROTOCOL_ID);
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
