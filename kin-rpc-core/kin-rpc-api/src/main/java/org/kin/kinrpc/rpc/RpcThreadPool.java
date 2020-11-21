package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.SysUtils;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RpcThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty以外, 都用这个线程池
     */
    private static ExecutionContext EXECUTORS;

    /**
     * provider 服务逻辑执行线程池
     */
    private static ExecutionContext PROVIDER_WORKER;

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(() -> {
            if (Objects.nonNull(EXECUTORS)) {
                EXECUTORS.shutdown();
            }
        });
        JvmCloseCleaner.DEFAULT().add(() -> {
            if (Objects.nonNull(PROVIDER_WORKER)) {
                PROVIDER_WORKER.shutdown();
            }
        });
    }

    /**
     * lazy init EXECUTORS
     */
    public static ExecutionContext executors() {
        if (Objects.isNull(EXECUTORS)) {
            synchronized (RpcThreadPool.class) {
                if (Objects.isNull(EXECUTORS)) {
                    EXECUTORS = new ExecutionContext(
                            //executor, 有界扩容的线程池, 允许线程数扩容到一定程度(10倍CPU核心数), 如果超过这个能力, 则buffer
                            new ThreadPoolExecutor(
                                    SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
                                    60L, TimeUnit.SECONDS,
                                    new LinkedBlockingQueue<>(),
                                    new SimpleThreadFactory("rpc-common")),
                            //scheduler
                            2, "rpc-common-scheduler");
                }
            }
        }
        return EXECUTORS;
    }

    /**
     * lazy init PROVIDER_WORKER
     */
    public static ExecutionContext providerWorkers() {
        if (Objects.isNull(PROVIDER_WORKER)) {
            synchronized (RpcThreadPool.class) {
                if (Objects.isNull(PROVIDER_WORKER)) {
                    PROVIDER_WORKER = new ExecutionContext(
                            //executor, 有界扩容的线程池, 允许线程数扩容到一定程度(10倍CPU核心数), 如果超过这个能力, 则buffer
                            new ThreadPoolExecutor(
                                    SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
                                    60L, TimeUnit.SECONDS,
                                    new LinkedBlockingQueue<>(),
                                    new SimpleThreadFactory("rpc-provider")));
                }
            }
        }
        return PROVIDER_WORKER;
    }
}
