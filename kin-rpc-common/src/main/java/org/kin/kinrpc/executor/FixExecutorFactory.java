package org.kin.kinrpc.executor;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.ExecutorConfig;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 固定线程数的线程池
 *
 * @author huangjianqin
 * @date 2021/4/20
 */
@Extension("fix")
public class FixExecutorFactory implements ExecutorFactory {
    @Override
    public ManagedExecutor create(ExecutorConfig config) {
        String name = config.getName();
        int queueSize = config.getQueueSize();
        int alive = config.getAlive();
        int corePoolSize = config.getCorePoolSize();
        return new DefaultManagedExecutor(
                ThreadPoolUtils.newThreadPool(name, true,
                        corePoolSize, corePoolSize, alive, TimeUnit.MILLISECONDS,
                        (queueSize <= 0 ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queueSize)),
                        new SimpleThreadFactory(name)));
    }
}