package org.kin.kinrpc.rpc.utils;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * Created by huangjianqin on 2019/6/12.
 */
public class ClassUtils {
    private ClassUtils() {

    }

    public static String getUniqueName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append("$");

        StringJoiner paramsJoiner = new StringJoiner("$");
        Class[] paramTypes = method.getParameterTypes();
        for (Class paramType : paramTypes) {
            paramsJoiner.add(paramType.getTypeName());
        }
        sb.append(paramsJoiner.toString());

        sb.append("$");
        return sb.toString();
    }
}
