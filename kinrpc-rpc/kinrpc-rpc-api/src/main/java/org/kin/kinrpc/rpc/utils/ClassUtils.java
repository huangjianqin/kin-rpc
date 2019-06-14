package org.kin.kinrpc.rpc.utils;

import java.lang.reflect.Method;

/**
 * Created by huangjianqin on 2019/6/12.
 */
public class ClassUtils {
    public static String getUniqueName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toString());
        return sb.toString();
    }
}
