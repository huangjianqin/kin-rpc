package org.kin.kinrpc.transport.protocol.domain;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class ResponseProtocol extends AbstractProtocol {
    public ResponseProtocol(short protocolId) {
        super(protocolId);
    }

    @Override
    public final void read(Request request) {
        //do nothing
    }
}