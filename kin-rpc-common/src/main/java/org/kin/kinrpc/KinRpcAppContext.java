package org.kin.kinrpc;

import org.kin.framework.utils.SysUtils;

import java.util.Objects;

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

    static {
        //判断当前环境是否支持增强代理
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
}
