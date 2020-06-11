package org.kin.kinrpc.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2020-06-07
 */
public class RpcEndpointThreadPool {
    /**
     * 公用线程池, 除了dispatcher以外, 都用这个线程池
     */
    public static final ExecutionContext EXECUTORS =
            ExecutionContext.forkjoin(SysUtils.getSuitableThreadNum(), "rpc-endpoint-common", 5, "rpc-endpoint-common-schedule");

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(EXECUTORS::shutdown);
    }
}
