package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.Executor;

/**
 * 在netty io线程处理rpc请求
 *
 * @author huangjianqin
 * @date 2021/4/19
 */
final class DirectExecutorFactory extends AbstractExecutorFactory {
    /** 直接调用 */
    private static final Executor DIRECT = Runnable::run;

    public DirectExecutorFactory(Url url, int port) {
        super(url, port);
    }

    @Override
    public Executor executor(RpcRequest rpcRequest, Channel channel) {
        return DIRECT;
    }

    @Override
    public void close() {
        //do nothing
    }
}
