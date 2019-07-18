package org.kin.kinrpc.transport.netty.protocol;

import org.kin.kinrpc.transport.netty.domain.Request;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class AbstractResponseProtocol extends AbstractProtocol {
    public AbstractResponseProtocol() {
    }

    public AbstractResponseProtocol(int protocolId) {
        super(protocolId);
    }

    @Override
    public final void read(Request request) {
        //do nothing
    }
}