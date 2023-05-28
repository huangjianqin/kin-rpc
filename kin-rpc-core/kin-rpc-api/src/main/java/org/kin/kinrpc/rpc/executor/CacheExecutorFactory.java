package org.kin.kinrpc.rpc.executor;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.rpc.common.AttributeKey;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.config.ExecutorConfig;

import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/4/20
 * @see Executors#newCachedThreadPool()
 */
@Extension("cache")
public class CacheExecutorFactory implements ExecutorFactory {
    @Override
    public Executor executor(String executorName, Url url) {
        ExecutorConfig config = url.getAttribute(AttributeKey.EXECUTOR);
        int threadSize = config.getThreadSize();
        int maxThreadSize = config.getMaxThreadSize();
        int alive = config.getAlive();

        return new ThreadPoolExecutor(threadSize, maxThreadSize, alive, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new SimpleThreadFactory(executorName));
    }
}
