package org.kin.kinrpc.rpc;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.HashedWheelTimer;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RpcThreadPool {
    /**
     * invoker公用线程池, 除了服务provider,netty以外, 都用这个线程池
     */
    private static final ExecutionContext EXECUTORS;
    /** 时间轮, 用于处理rpc call超时 */
    private static final HashedWheelTimer WHEEL_TIMER;

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        //lazy init EXECUTORS
        EXECUTORS = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 5,
                "rpc-reference", 2);

        //200ms tick
        WHEEL_TIMER = new HashedWheelTimer(200, TimeUnit.MILLISECONDS);

        JvmCloseCleaner.instance().add(EXECUTORS::shutdown);
        JvmCloseCleaner.instance().add(WHEEL_TIMER::stop);
    }

    /**
     * @return reference端的公共线程池
     */
    public static ExecutionContext executors() {
        return EXECUTORS;
    }

    /**
     * @return reference端的时间轮
     */
    public static HashedWheelTimer wheelTimer() {
        return WHEEL_TIMER;
    }
}
