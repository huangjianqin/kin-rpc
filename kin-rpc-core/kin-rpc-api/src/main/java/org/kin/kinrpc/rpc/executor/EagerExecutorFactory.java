package org.kin.kinrpc.rpc.executor;

import org.kin.framework.concurrent.EagerThreadPoolExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.rpc.common.AttributeKey;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.config.ExecutorConfig;

import java.util.concurrent.Executor;
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
    public Executor executor(String executorName, Url url) {
        ExecutorConfig config = url.getAttribute(AttributeKey.EXECUTOR);
        int threadSize = config.getThreadSize();
        int maxThreadSize = config.getMaxThreadSize();
        int queueSize = config.getQueueSize();
        int alive = config.getAlive();

        return EagerThreadPoolExecutor.create(threadSize, maxThreadSize, alive, TimeUnit.MILLISECONDS, queueSize <= 0 ? 1 : queueSize,
                new SimpleThreadFactory(executorName));
    }
}
