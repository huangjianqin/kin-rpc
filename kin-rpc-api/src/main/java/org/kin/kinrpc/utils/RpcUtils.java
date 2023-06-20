package org.kin.kinrpc.utils;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public final class RpcUtils {
    private RpcUtils() {
    }

    /**
     * 服务方法是否合法
     *
     * @param method 服务方法元数据
     * @return true表示服务方法合法
     */
    public static boolean isRpcMethodValid(Method method) {
        Class<?> returnType = method.getReturnType();
        return Future.class.isAssignableFrom(returnType);
    }
}
