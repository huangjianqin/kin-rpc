package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RpcThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty以外, 都用这个线程池
     */
    private static final ExecutionContext EXECUTORS;

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        //lazy init EXECUTORS
        EXECUTORS = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
                "rpc-reference", 2, "rpc-reference-scheduler");

        JvmCloseCleaner.DEFAULT().add(EXECUTORS::shutdown);
    }

    /**
     * @return reference端的公共线程池
     */
    public static ExecutionContext executors() {
        return EXECUTORS;
    }
}
