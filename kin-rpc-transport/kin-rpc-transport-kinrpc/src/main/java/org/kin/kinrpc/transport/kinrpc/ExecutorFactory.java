package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import org.kin.framework.Closeable;

import java.util.concurrent.Executor;

/**
 * kinrpc底层netty server业务处理线程池
 *
 * @author huangjianqin
 * @date 2021/4/20
 */
public interface ExecutorFactory extends Closeable {
    /**
     * 返回本次请求处理的executor
     *
     * @param rpcRequest rpc请求
     * @param channel    netty channel
     * @return 返回本次请求处理的executor
     */
    Executor executor(RpcRequest rpcRequest, Channel channel);
}
