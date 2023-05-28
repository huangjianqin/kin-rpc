package org.kin.kinrpc.rpc.executor;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.Executor;

/**
 * 直接在调用线程执行的{@link Executor}实现
 *
 * @author huangjianqin
 * @date 2021/4/19
 */
@Extension("direct")
public class DirectExecutorFactory implements ExecutorFactory {
    /** 直接调用 */
    private static final Executor DIRECT = Runnable::run;

    @Override
    public Executor executor(String executorName, Url url) {
        return DIRECT;
    }
}
