package org.kin.kinrpc.registry;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2019-08-22
 */
public class RegistryThreadPool {
    /**
     * registry公用线程池
     */
    public static final ExecutionContext EXECUTORS =
            ExecutionContext.forkjoin(SysUtils.getSuitableThreadNum(), "rpc-registry", 1, "rpc-registry-schedule");

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        JvmCloseCleaner.DEFAULT().add(EXECUTORS::shutdown);
    }
}
