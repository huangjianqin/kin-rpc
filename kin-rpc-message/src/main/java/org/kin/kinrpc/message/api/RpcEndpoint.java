package org.kin.kinrpc.message.api;

import org.kin.framework.concurrent.actor.Receiver;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcEndpoint extends Receiver<RpcMessageCallContext> {
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void receive(RpcMessageCallContext context) {
        //default do nothing
    }
}
