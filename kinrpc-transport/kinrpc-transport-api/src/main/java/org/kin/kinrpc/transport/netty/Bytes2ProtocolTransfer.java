package org.kin.kinrpc.transport.netty;

import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/3.
 */
@FunctionalInterface
public interface Bytes2ProtocolTransfer {
    <T extends AbstractProtocol> T transfer(Request request);
}
