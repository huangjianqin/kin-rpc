package org.kin.kinrpc.transport.protocol.domain;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class RequestProtocol extends AbstractProtocol {
    public RequestProtocol(short protocolId) {
        super(protocolId);
    }

    @Override
    public final void write(Response response) {
        //do nothing
    }
}
