package org.kin.kinrpc.transport.netty.protocol;

import org.kin.kinrpc.transport.netty.domain.ProtocolByteBuf;
import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.domain.Response;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class AbstractProtocol {
    private int protocolId;
    private long createTime = System.currentTimeMillis();

    public AbstractProtocol() {
    }

    public AbstractProtocol(int protocolId) {
        this.protocolId = protocolId;
    }

    protected void beforeRead(Request request) {

    }

    public abstract void read(Request request);

    public abstract void write(Response response);

    public Response write() {
        Response response = new ProtocolByteBuf(protocolId);
        write(response);
        return response;
    }

    @Override
    public String toString() {
        return "Protocol<" + getClass().getSimpleName() + ">{" +
                "protocolId=" + protocolId +
                ", createTime=" + createTime +
                '}';
    }

    //setter && getter
    public int getProtocolId() {
        return protocolId;
    }

    public long getCreateTime() {
        return createTime;
    }
}
