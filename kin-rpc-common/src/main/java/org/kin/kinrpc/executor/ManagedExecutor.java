package org.kin.kinrpc.executor;

import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface ManagedExecutor extends Executor {
    /** 关闭线程池并释放资源 */
    void shutdown();
}
