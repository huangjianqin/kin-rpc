package org.kin.kinrpc.registry;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.ForkJoinPool;

/**
 * @author huangjianqin
 * @date 2019-08-22
 */
public class RegistryThreadPool {
    /**
     * registry公用线程池
     */
    public static final ThreadManager THREADS = new ThreadManager(
            new ForkJoinPool(SysUtils.getSuitableThreadNum(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null,
                    true
            ), 1, new SimpleThreadFactory("registry-schedule"));

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(() -> THREADS.shutdown());
    }
}
