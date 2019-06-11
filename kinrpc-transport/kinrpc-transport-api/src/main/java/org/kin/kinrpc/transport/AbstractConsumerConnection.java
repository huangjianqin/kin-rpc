package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public abstract class AbstractConsumerConnection extends Connection {
    public AbstractConsumerConnection(InetSocketAddress address) {
        super(address);
    }

    public abstract Future<RPCResponse> request(RPCRequest rpcRequest);

    public abstract boolean isActive();
}
