package org.kin.kinrpc.rpc.domain;

import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/13.
 */
public class RPCContext {
    private ThreadLocal<Future> rpcFuture = new ThreadLocal<>();
    private static final RPCContext INSTANCE = new RPCContext();

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
