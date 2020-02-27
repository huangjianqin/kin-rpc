package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.ForkJoinPool;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RPCThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty和服务注册以外, 都用这个线程池
     */
    public static final ThreadManager THREADS = new ThreadManager(
            new ForkJoinPool(SysUtils.getSuitableThreadNum() * 2 - 1,
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null,
                    true
            ), 5, new SimpleThreadFactory("rpc-schedule"));

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(THREADS::shutdown);
    }

}
