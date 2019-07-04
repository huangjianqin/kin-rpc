package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.Protocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
@Protocol(id = 1)
public class Protocol1 extends AbstractProtocol{
    private int f;

    @Override
    public void read(Request request) {
        f = request.readInt();
    }

    @Override
    public void write(Response response) {
        response.writeInt(f);
    }

    @Override
    public String toString() {
        return super.toString() + "Protocol1{" +
                "f=" + f +
                '}';
    }
}
