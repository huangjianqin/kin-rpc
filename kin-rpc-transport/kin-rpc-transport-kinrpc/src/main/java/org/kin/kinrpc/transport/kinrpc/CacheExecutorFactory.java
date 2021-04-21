package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/4/20
 */
final class CacheExecutorFactory extends AbstractExecutorFactory {
    private final ExecutorService executor;

    protected CacheExecutorFactory(Url url, int port) {
        super(url, port);

        int cores = url.getIntParam(Constants.CORES_KEY, Constants.CORES);
        int maxThreads = url.getIntParam(Constants.MAX_THREADS_KEY, Constants.MAX_THREADS);
        int alive = url.getIntParam(Constants.ALIVE_KEY, Constants.ALIVE);

        executor = new ThreadPoolExecutor(cores, maxThreads, alive, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new SimpleThreadFactory("kinrpc-provider-" + port));
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
