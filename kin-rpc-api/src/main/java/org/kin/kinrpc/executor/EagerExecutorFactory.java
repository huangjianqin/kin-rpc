package org.kin.kinrpc.executor;

import org.kin.framework.concurrent.EagerThreadPoolExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.ExecutorConfig;

import java.util.concurrent.TimeUnit;

/**
 * 优先创建多个线程处理rpc请求, 如果达到线程池最大线程数限制, 则入队等待
 *
 * @author huangjianqin
 * @date 2021/4/19
 * @see org.kin.framework.concurrent.EagerThreadPoolExecutor
 */
@Extension("eager")
public class EagerExecutorFactory implements ExecutorFactory {
    @Override
    public ServiceExecutor create(ExecutorConfig config) {
        String name = config.getName();
        int corePoolSize = config.getCorePoolSize();
        int maxPoolSize = config.getMaxPoolSize();
        int queueSize = config.getQueueSize();
        int alive = config.getAlive();

        return new DefaultServiceExecutor(
                EagerThreadPoolExecutor.create(corePoolSize, maxPoolSize, alive, TimeUnit.MILLISECONDS, queueSize <= 0 ? 1 : queueSize,
                        new SimpleThreadFactory(name)));
    }
}
