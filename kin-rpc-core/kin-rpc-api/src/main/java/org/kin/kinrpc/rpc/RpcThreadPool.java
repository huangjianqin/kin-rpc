package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RpcThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty以外, 都用这个线程池
     */
    public static final ExecutionContext EXECUTORS =
            ExecutionContext.cache("rpc-common", 5, "rpc-common-schedule");

    /**
     * 服务请求处理线程池
     */
    public static ExecutionContext PROVIDER_WORKER =
            ExecutionContext.cache("rpc-provider", 2, "rpc-provider-schedule");

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(EXECUTORS::shutdown);
        JvmCloseCleaner.DEFAULT().add(PROVIDER_WORKER::shutdown);

    }

}
