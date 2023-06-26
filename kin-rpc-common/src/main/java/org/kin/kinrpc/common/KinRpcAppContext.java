package org.kin.kinrpc.common;

import org.kin.framework.event.DefaultEventBus;
import org.kin.framework.event.EventBus;

import java.util.Objects;
import java.util.UUID;

/**
 * @author huangjianqin
 * @date 2023/6/12
 */
public class KinRpcAppContext {
    // TODO: 2023/6/25
    /** app uuid */
    public static final String ID = UUID.randomUUID().toString();
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

    /**
     * dispatch event
     * @param event 时间
     */
    public static void dispatchEvent(Object event){
        EVENT_BUS.post(event);
    }
}
