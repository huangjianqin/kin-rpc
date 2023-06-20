package org.kin.kinrpc.executor;

import org.kin.kinrpc.transport.ExecutorSelector;

import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface ServiceExecutor extends Executor, ExecutorSelector {
    /** 关闭线程池并释放资源 */
    void shutdown();
}
