package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;

import java.util.Objects;

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
                    /**
                     * kinrpc协议全调用链支持异步操作故可以适当减少线程数量, 但在同步rpc call时, 仍然存在阻塞, 所以用cache保证有足够线程处理请求
                     *
                     * 其他协议:
                     * 1. http rpc call是会阻塞等待, 所以还是需要cache
                     */
                    EXECUTORS = ExecutionContext.cache(
                            SysUtils.getSuitableThreadNum(), Integer.MAX_VALUE, "rpc-common",
                            5, "rpc-common-schedule");
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
                    //provider服务存在future阻塞等待操作, 所以用cache
                    PROVIDER_WORKER =
                            ExecutionContext.cache("rpc-provider");
                }
            }
        }
        return PROVIDER_WORKER;
    }
}
