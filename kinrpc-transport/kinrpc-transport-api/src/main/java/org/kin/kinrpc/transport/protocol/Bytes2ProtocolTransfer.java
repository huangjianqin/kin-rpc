package org.kin.kinrpc.transport.protocol;

import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.domain.Request;

/**
 * Created by huangjianqin on 2019/6/3.
 */
@FunctionalInterface
public interface Bytes2ProtocolTransfer {
    <T extends AbstractProtocol> T transfer(Request request);
}
