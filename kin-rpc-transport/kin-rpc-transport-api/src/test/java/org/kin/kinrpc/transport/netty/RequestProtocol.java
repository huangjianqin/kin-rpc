package org.kin.kinrpc.transport.netty;

import org.kin.kinrpc.transport.netty.domain.Response;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public abstract class RequestProtocol extends AbstractProtocol {
    @Override
    public void write(Response response) {

    }
}
