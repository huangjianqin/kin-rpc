package org.kin.kinrpc.rpc;

import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/13.
 */
public class RPCContext {
    private static final RPCContext INSTANCE = new RPCContext();

    private ThreadLocal<Future> rpcFuture = new ThreadLocal<>();

    private RPCContext(){
    }

    public static RPCContext instance() {
        return INSTANCE;
    }

    //setter && getter
    public Future getFuture() {
        return rpcFuture.get();
    }

    public void setFuture(Future future) {
        rpcFuture.set(future);
    }
}
