package org.kin.kinrpc.transport.netty;

import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.protocol.Protocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
@Protocol(id = 2)
public class Protocol2 extends RequestProtocol {
    private String s;
    private byte b;

    @Override
    public void read(Request request) {
        s = request.readString();
        b = request.readByte();
    }

    @Override
    public String toString() {
        return super.toString() + "Protocol2{" +
                "s='" + s + '\'' +
                ", b=" + b +
                '}';
    }
}
