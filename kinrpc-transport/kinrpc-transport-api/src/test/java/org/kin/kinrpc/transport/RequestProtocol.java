package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public abstract class RequestProtocol extends AbstractProtocol {
    @Override
    public void write(Response response) {

    }
}
