package org.kin.kinrpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.event.DefaultEventBus;
import org.kin.framework.event.EventBus;
import org.kin.framework.utils.SysUtils;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * kin rpc app 上下文
 *
 * @author huangjianqin
 * @date 2023/6/12
 */
public final class KinRpcAppContext {
    /** 进程pid */
    public static final int PID = SysUtils.getPid();
    /** 是否支持字节码增强 */
    public static final boolean ENHANCE;
    /** kinrpc应用通用scheduler */
    public static final ExecutionContext SCHEDULER;
    /** 默认event bus */
    private static final EventBus EVENT_BUS = DefaultEventBus.create();

    static {
        //判断当前环境是否支持增强代理
        Class<?> byteBuddyClass = null;
        try {
            byteBuddyClass = Class.forName("net.bytebuddy.ByteBuddy");
        } catch (Exception e) {
            //ignore
        }

        ENHANCE = Objects.nonNull(byteBuddyClass);

        //通用scheduler
        ThreadPoolExecutor worker = ThreadPoolUtils.newThreadPool("kinrpc-common-worker", true,
                SysUtils.CPU_NUM, SysUtils.DOUBLE_CPU,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256), new SimpleThreadFactory("kinrpc-common-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        worker.allowCoreThreadTimeOut(true);

        ScheduledExecutorService scheduler = ThreadPoolUtils.newScheduledThreadPool("kinrpc-common-scheduler", true,
                SysUtils.CPU_NUM / 2 + 1,
                new SimpleThreadFactory("kinrpc-common-scheduler", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        SCHEDULER = new ExecutionContext(worker, scheduler);

        JvmCloseCleaner.instance().add(SCHEDULER::shutdown);
    }

    private KinRpcAppContext() {
    }

    /**
     * dispatch event
     *
     * @param event 时间
     */
    public static void dispatchEvent(Object event) {
        EVENT_BUS.post(event);
    }
}
