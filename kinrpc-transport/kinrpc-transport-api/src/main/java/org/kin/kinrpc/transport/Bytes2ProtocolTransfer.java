package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.AbstractProtocol;
import org.kin.kinrpc.transport.domain.Request;

/**
 * Created by huangjianqin on 2019/6/3.
 */
@FunctionalInterface
public interface Bytes2ProtocolTransfer {
    <T extends AbstractProtocol> T transfer(Request request);
}
