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
 * @author huangjianqin
 * @date 2023/7/21
 */
public final class RegistryContext {
    /** registry scheduler */
    public static final ExecutionContext SCHEDULER;

    static {
        ThreadPoolExecutor registryWorker = ThreadPoolUtils.newThreadPool("kinrpc-registry", true,
                SysUtils.DOUBLE_CPU, SysUtils.DOUBLE_CPU,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256), new SimpleThreadFactory("kinrpc-registry", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        registryWorker.allowCoreThreadTimeOut(true);

        ScheduledExecutorService discoveryScheduler = ThreadPoolUtils.newScheduledThreadPool("kinrpc-registry-scheduler", true,
                2,
                new SimpleThreadFactory("kinrpc-registry-scheduler", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        SCHEDULER = new ExecutionContext(registryWorker, discoveryScheduler);


        //hook
        JvmCloseCleaner.instance().add(SCHEDULER::shutdown);
    }


    private RegistryContext() {
    }
}
