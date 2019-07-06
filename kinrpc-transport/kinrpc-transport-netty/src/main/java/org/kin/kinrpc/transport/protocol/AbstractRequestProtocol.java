package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.domain.Response;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class AbstractRequestProtocol extends AbstractProtocol {
    public AbstractRequestProtocol() {
    }

    public AbstractRequestProtocol(int protocolId) {
        super(protocolId);
    }

    @Override
    public final void write(Response response) {
        //do nothing
    }
}