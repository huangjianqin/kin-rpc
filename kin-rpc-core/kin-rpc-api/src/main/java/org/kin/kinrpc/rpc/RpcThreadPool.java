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

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(() -> {
            if (Objects.nonNull(EXECUTORS)) {
                EXECUTORS.shutdown();
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
                    EXECUTORS = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
                            "rpc-common", 2, "rpc-common-scheduler");
                }
            }
        }
        return EXECUTORS;
    }
}
