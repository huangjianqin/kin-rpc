package org.kin.kinrpc.message.core;

import org.kin.framework.concurrent.Receiver;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.kinrpc.transport.kinrpc.KinRpcRequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class RpcEndpoint extends Receiver<RpcCallContext> {
    private static final Logger msgLog = LoggerFactory.getLogger(RpcEndpoint.class);
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
    public final void receive(RpcCallContext context) {
        //更新该线程本地的rpc环境
        RpcEnv.updateCurrentRpcEnv(rpcEnv);
        //设置message handle时间
        context.setHandleTime(System.currentTimeMillis());

        //log 一些消息日志
        KinRpcAddress fromAddress = context.getFromAddress();
        RpcEndpointRef to = context.getTo();
        msgLog.info("receive message from {} to {}({}), {}-{}-{}-{}-{}",
                fromAddress.address(),
                Objects.nonNull(to) ? to.getEndpointAddress().getName() : "internal",
                Objects.nonNull(to) ? to.getEndpointAddress().getRpcAddress().address() : "internal",
                context.getRequestId(),
                context.getCreateTime(), context.getEventTime(), context.getHandleTime(),
                context.getMessage()
        );
        onReceiveMessage(context);
    }

    /**
     * 处理消息逻辑实现
     */
    protected abstract void onReceiveMessage(RpcCallContext context);

    /**
     * 标识是否线程安全
     */
    public boolean threadSafe() {
        return false;
    }

    /**
     * 获取指向这个RpcEndpoint的RpcEndpointRef
     */
    public final RpcEndpointRef ref() {
        return rpcEnv.rpcEndpointRef(this);
    }

    /**
     * 给自己分派消息
     */
    public final void send2Self(Serializable message) {
        rpcEnv.postMessage(RpcMessage.of(KinRpcRequestIdGenerator.next(), rpcEnv.address(), rpcEnv.rpcEndpointRef(this), message));
    }

}
