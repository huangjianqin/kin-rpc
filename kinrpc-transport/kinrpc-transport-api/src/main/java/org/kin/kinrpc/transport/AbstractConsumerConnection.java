package org.kin.kinrpc.transport;

import org.kin.kinrpc.future.RPCFuture;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;

import java.net.InetSocketAddress;

/**
 * Created by huangjianqin on 2019/6/5.
 */
public abstract class AbstractConsumerConnection extends Connection {
    public AbstractConsumerConnection(InetSocketAddress address) {
        super(address);
    }

    public abstract RPCFuture request(RPCRequest rpcRequest);
}
