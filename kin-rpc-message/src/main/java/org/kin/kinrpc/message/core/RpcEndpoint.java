package org.kin.kinrpc.message.core;

import org.kin.framework.concurrent.actor.Receiver;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcEndpoint extends Receiver<RpcMessageCallContext> {
    private static final Logger msgLog = LoggerFactory.getLogger("message");
    /**
     *
     */
    private RpcEnv rpcEnv;

    /**
     * 更新rpc环境
     */
    public void updateRpcEnv(RpcEnv rpcEnv) {
        this.rpcEnv = rpcEnv;
    }

    public RpcEndpointRef ref() {
        return rpcEnv.rpcEndpointRef(this);
    }

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
        context.setHandleTime(System.currentTimeMillis());

        RpcAddress fromAddress = context.getFromAddress();
        RpcEndpointRef to = context.getTo();
        msgLog.info("receive message from {} to {}({}), {}-{}-{}-{}-{}",
                fromAddress.str(),
                to.getEndpointAddress().getName(), to.getEndpointAddress().getRpcAddress().str(),
                context.getRequestId(),
                context.getCreateTime(), context.getEventTime(), context.getHandleTime(),
                context.getMessage()
        );
    }

    public boolean threadSafe() {
        return false;
    }
}
