package org.kin.kinrpc.cluster.utils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.kin.framework.utils.ExceptionUtils;

/**
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class ByteBuddyUtils {

    private ByteBuddyUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> serviceInterface, Object proxy) {
        Class<T> proxyClass = (Class<T>) new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(serviceInterface)
                .name(serviceInterface.getSimpleName() + "Reference")
                //过滤默认方法和Object定义的方法
                .method(ElementMatchers.not(ElementMatchers.isDefaultMethod())
                        .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
                .intercept(MethodDelegation.to(proxy))
                .make()
                .load(serviceInterface.getClassLoader())
                .getLoaded();

        try {
            return proxyClass.newInstance();
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }
}
