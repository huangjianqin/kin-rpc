package org.kin.kinrpc.rpc.executor;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.rpc.common.AttributeKey;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.config.ExecutorConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 固定线程数的线程池
 * @author huangjianqin
 * @date 2021/4/20
 */
@Extension("fix")
public class FixExecutorFactory implements ExecutorFactory {
    @Override
    public Executor executor(String executorName, Url url) {
        ExecutorConfig config = url.getAttribute(AttributeKey.EXECUTOR);
        int queueSize = config.getQueueSize();
        int alive = config.getAlive();
        int threadSize = config.getThreadSize();
        return new ThreadPoolExecutor(threadSize, threadSize, alive, TimeUnit.MILLISECONDS,
                (queueSize <= 0 ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queueSize)),
                new SimpleThreadFactory(executorName));
    }
}