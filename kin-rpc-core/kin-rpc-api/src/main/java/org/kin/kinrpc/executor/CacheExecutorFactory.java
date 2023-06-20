package org.kin.kinrpc.executor;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.ExecutorConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/4/20
 * @see Executors#newCachedThreadPool()
 */
@Extension("cache")
public class CacheExecutorFactory implements ExecutorFactory {
    @Override
    public ServiceExecutor create(ExecutorConfig config) {
        String name = config.getName();
        int corePoolSize = config.getCorePoolSize();
        int maxPoolSize = config.getMaxPoolSize();
        int alive = config.getAlive();

        return new DefaultServiceExecutor(
                new ThreadPoolExecutor(corePoolSize, maxPoolSize, alive, TimeUnit.MILLISECONDS,
                        new SynchronousQueue<>(), new SimpleThreadFactory(name)));
    }
}
