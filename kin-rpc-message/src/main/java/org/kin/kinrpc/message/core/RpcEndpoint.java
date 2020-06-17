package org.kin.kinrpc.message.core;

import org.kin.framework.concurrent.actor.Receiver;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.domain.RpcRequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcEndpoint extends Receiver<RpcMessageCallContext> {
    private static final Logger msgLog = LoggerFactory.getLogger("message");
    /** rpc环境 */
    protected final RpcEnv rpcEnv;

    public RpcEndpoint(RpcEnv rpcEnv) {
        this.rpcEnv = rpcEnv;
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
        //更新该线程的rpc环境
        RpcEnv.updateCurrentRpcEnv(rpcEnv);
        //default do nothing
        context.setHandleTime(System.currentTimeMillis());

        //log 一些消息日志
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

    /**
     * 标识是否线程安全
     */
    public boolean threadSafe() {
        return false;
    }

    /**
     * 获取指向这个RpcEndpoint的RpcEndpointRef
     */
    public RpcEndpointRef ref() {
        return rpcEnv.rpcEndpointRef(this);
    }

    /**
     * 分派并处理接受到的消息
     */
    public void send2Self(Serializable message) {
        rpcEnv.postMessage(RpcMessage.of(RpcRequestIdGenerator.next(), rpcEnv.address(), rpcEnv.rpcEndpointRef(this), message));
    }

}
