package org.kin.kinrpc.executor;

import java.util.concurrent.ExecutorService;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public class DefaultServiceExecutor implements ServiceExecutor {
    private final ExecutorService executor;

    public DefaultServiceExecutor(ExecutorService executor) {
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
