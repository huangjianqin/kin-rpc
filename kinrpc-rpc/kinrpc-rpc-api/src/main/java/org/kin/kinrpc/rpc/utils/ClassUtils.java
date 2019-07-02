package org.kin.kinrpc.rpc.utils;

import java.lang.reflect.Method;

/**
 * Created by huangjianqin on 2019/6/12.
 */
public class ClassUtils {
    private ClassUtils(){

    }

    public static String getUniqueName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append("(");
        Class[] paramTypes = method.getParameterTypes();
        for(Class paramType: paramTypes){
            sb.append(paramType.getTypeName() + ",");
        }
        if(paramTypes.length > 0){
            sb.replace(sb.length() - 1, sb.length(), "");
        }
        sb.append(")");
        return sb.toString();
    }
}
