package org.kin.kinrpc;

import org.kin.framework.event.DefaultEventBus;
import org.kin.framework.event.EventBus;
import org.kin.framework.utils.SysUtils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/12
 */
public final class KinRpcAppContext {
    /** 进程pid */
    public static final int PID = SysUtils.getPid();
    /** 是否支持字节码增强 */
    public static final boolean ENHANCE;
    /** 默认event bus */
    private static final EventBus EVENT_BUS = DefaultEventBus.create();

    static {
        Class<?> byteBuddyClass = null;
        try {
            byteBuddyClass = Class.forName("net.bytebuddy.ByteBuddy");
        } catch (Exception e) {
            //ignore
        }

        ENHANCE = Objects.nonNull(byteBuddyClass);
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
