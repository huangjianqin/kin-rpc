package org.kin.kinrpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * reference上下文, 定义reference共享实例数据
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class ReferenceContext {
    /** reference端通用scheduler */
    public static final ExecutionContext SCHEDULER;

    static {
        ThreadPoolExecutor worker = ThreadPoolUtils.newThreadPool("kinrpc-reference-worker", true,
                SysUtils.DOUBLE_CPU, SysUtils.DOUBLE_CPU,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256), new SimpleThreadFactory("kinrpc-reference-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        worker.allowCoreThreadTimeOut(true);

        ScheduledExecutorService scheduler = ThreadPoolUtils.newScheduledThreadPool("kinrpc-reference-scheduler", true,
                SysUtils.CPU_NUM / 2 + 1,
                new SimpleThreadFactory("kinrpc-reference-scheduler", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        SCHEDULER = new ExecutionContext(worker, scheduler);

        //hook
        JvmCloseCleaner.instance().add(SCHEDULER::shutdown);
    }

    private ReferenceContext() {
    }
}
