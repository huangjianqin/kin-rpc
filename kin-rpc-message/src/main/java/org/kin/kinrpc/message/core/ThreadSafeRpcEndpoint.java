package org.kin.kinrpc.message.core;

/**
 * 线程安全的RpcEndpoint
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public abstract class ThreadSafeRpcEndpoint extends RpcEndpoint {
    public ThreadSafeRpcEndpoint(RpcEnv rpcEnv) {
        super(rpcEnv);
    }

    @Override
    public final boolean threadSafe() {
        return true;
    }
}
