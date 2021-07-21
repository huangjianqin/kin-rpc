package org.kin.kinrpc.transport.kinrpc;

import org.kin.transport.netty.socket.protocol.Protocol;
import org.kin.transport.netty.socket.protocol.SocketProtocol;

/**
 * 相对server而言, 响应协议
 * Created by huangjianqin on 2019/6/14.
 */
@Protocol(id = KinRpcProtocolId.RPC_RESPONSE_PROTOCOL_ID)
public class KinRpcResponseProtocol extends SocketProtocol {
    /** request唯一id */
    private long requestId;
    /** 序列化类型 */
    private byte serialization;
    /** content */
    private byte[] respContent;

    public static KinRpcResponseProtocol create(long requestId, byte serialization, byte[] respContent) {
        KinRpcResponseProtocol protocol = new KinRpcResponseProtocol();
        protocol.requestId = requestId;
        protocol.serialization = serialization;
        protocol.respContent = respContent;
        return protocol;
    }

    //setter && getter
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte getSerialization() {
        return serialization;
    }

    public void setSerialization(byte serialization) {
        this.serialization = serialization;
    }

    public byte[] getRespContent() {
        return respContent;
    }

    public void setRespContent(byte[] respContent) {
        this.respContent = respContent;
    }
}
