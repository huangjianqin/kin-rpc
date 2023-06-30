package org.kin.kinrpc.executor;

import java.util.concurrent.ExecutorService;

/**
 * 将{@link ExecutorService}包装成{@link ManagedExecutor}实例
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class DefaultManagedExecutor implements ManagedExecutor {
    /** executor service */
    private final ExecutorService executor;

    public DefaultManagedExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }
}
