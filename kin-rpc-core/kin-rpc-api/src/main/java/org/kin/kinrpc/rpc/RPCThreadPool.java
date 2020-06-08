package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RPCThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty以外, 都用这个线程池
     */
    public static final ExecutionContext EXECUTORS =
            ExecutionContext.forkjoin(SysUtils.getSuitableThreadNum(), "rpc-common", 5, "rpc-common-schedule");

    /**
     * 服务请求处理线程池
     * 可以在加载类RPCProvider前修改RPC.parallelism来修改RPCProvider的并发数
     */
    public static ExecutionContext PROVIDER_WORKER =
            ExecutionContext.fix(SysUtils.getSuitableThreadNum(), "rpc-provider", 2, "rpc-provider-schedule");

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(EXECUTORS::shutdown);
        JvmCloseCleaner.DEFAULT().add(PROVIDER_WORKER::shutdown);

    }

}
