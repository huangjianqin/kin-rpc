package org.kin.kinrpc.executor;

import java.util.concurrent.Executor;

/**
 * 支持shutdown的{@link Executor}接口定义
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface ManagedExecutor extends Executor {
    /** 关闭线程池并释放资源 */
    void shutdown();
}
