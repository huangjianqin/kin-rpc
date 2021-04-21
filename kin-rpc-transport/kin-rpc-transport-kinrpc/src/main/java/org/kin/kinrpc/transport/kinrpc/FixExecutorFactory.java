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
final class FixExecutorFactory extends AbstractExecutorFactory {
    private final ExecutorService executor;

    protected FixExecutorFactory(Url url, int port) {
        super(url, port);

        int cores = url.getIntParam(Constants.CORES_KEY, Constants.CORES);
        int queues = url.getIntParam(Constants.QUEUES_KEY, Constants.QUEUES);

        executor = new ThreadPoolExecutor(cores, cores, 0, TimeUnit.MILLISECONDS,
                (queues <= 0 ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queues)),
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