package org.kin.kinrpc.message.core;

/**
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ThreadSafeRpcEndpoint extends RpcEndpoint {
    public ThreadSafeRpcEndpoint(RpcEnv rpcEnv) {
        super(rpcEnv);
    }

    @Override
    public final boolean threadSafe() {
        return true;
    }
}
