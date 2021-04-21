package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import org.kin.framework.concurrent.EagerThreadPoolExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 多线程处理rpc请求
 * 底层是基于{@link org.kin.framework.concurrent.EagerThreadPoolExecutor}, 优先创建多个线程处理rpc请求, 如果仍然处理不过来, 则入队
 *
 * @author huangjianqin
 * @date 2021/4/19
 * @see org.kin.framework.concurrent.EagerThreadPoolExecutor
 */
final class EagerExecutorFactory extends AbstractExecutorFactory {
    private final ExecutorService executor;

    protected EagerExecutorFactory(Url url, int port) {
        super(url, port);

        int cores = url.getIntParam(Constants.CORES_KEY, Constants.CORES);
        int maxThreads = url.getIntParam(Constants.MAX_THREADS_KEY, Constants.MAX_THREADS);
        int queues = url.getIntParam(Constants.QUEUES_KEY, Constants.QUEUES);
        int alive = url.getIntParam(Constants.ALIVE_KEY, Constants.ALIVE);

        executor = EagerThreadPoolExecutor.create(cores, maxThreads, alive, TimeUnit.MILLISECONDS, queues <= 0 ? 1 : queues,
                new SimpleThreadFactory("kinrpc-provider-" + port));
    }

    @Override
    public Executor executor(RpcRequest rpcRequest, Channel channel) {
        return executor;
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
