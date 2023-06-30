package org.kin.kinrpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.HashedWheelTimer;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class ReferenceContext {
    /** reference端通用线程池 */
    public static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(SysUtils.DOUBLE_CPU, SysUtils.DOUBLE_CPU,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(256),
                    new SimpleThreadFactory("kinrpc-reference-exec", false),
                    new ThreadPoolExecutor.CallerRunsPolicy());
    /** reference端timer */
    public static final HashedWheelTimer TIMER = new HashedWheelTimer(new SimpleThreadFactory("kinrpc-reference-timer", true),
            60, TimeUnit.SECONDS);

    static {
        //允许core thread timeout
        EXECUTOR.allowCoreThreadTimeOut(true);

        //hook
        JvmCloseCleaner.instance().add(() -> {
            EXECUTOR.shutdown();
            TIMER.stop();
        });
    }

    private ReferenceContext() {
    }
}
