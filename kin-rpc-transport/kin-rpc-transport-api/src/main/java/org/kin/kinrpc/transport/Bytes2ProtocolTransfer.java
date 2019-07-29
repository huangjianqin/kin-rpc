package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * Created by huangjianqin on 2019/6/3.
 */
@FunctionalInterface
public interface Bytes2ProtocolTransfer {
    <T extends AbstractProtocol> T transfer(Request request);
}
