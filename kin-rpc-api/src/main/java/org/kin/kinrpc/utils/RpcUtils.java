package org.kin.kinrpc.utils;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.kin.kinrpc.DefaultMethodMetadata;
import org.kin.kinrpc.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public final class RpcUtils {
    private static final Logger log = LoggerFactory.getLogger(RpcUtils.class);

    private RpcUtils() {
    }

    /**
     * 服务方法是否合法
     *
     * @param method 服务方法元数据
     * @return true表示服务方法合法
     */
    public static boolean isRpcMethodValid(Method method) {
        String uniqueName = getUniqueName(method);

        if (Object.class.equals(method.getDeclaringClass())) {
            //过滤Object定义的方法
            return false;
        }

        Class<?> returnType = method.getReturnType();
        if (Future.class.isAssignableFrom(returnType) && !CompletableFuture.class.isAssignableFrom(returnType)) {
            log.warn("service method '{}' is ignore, due to it is invalid", uniqueName);
            return false;
        }

        return true;
    }

    /**
     * 返回unique name
     *
     * @param method 服务方法
     * @return unique name
     */
    public static String getUniqueName(Method method) {
        return method.getName();
    }

    /**
     * 返回服务方法元数据
     *
     * @param service        服务唯一标识
     * @param interfaceClass 服务接口
     * @return 服务方法元数据
     */
    public static IntObjectMap<MethodMetadata> getMethodMetadataMap(String service,
                                                                    Class<?> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        IntObjectHashMap<MethodMetadata> methodMetadataMap = new IntObjectHashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            if (!RpcUtils.isRpcMethodValid(method)) {
                continue;
            }

            MethodMetadata metadata = new DefaultMethodMetadata(service, method);
            methodMetadataMap.put(metadata.handlerId(), metadata);
        }

        return methodMetadataMap;
    }

    /**
     * 包装rpc call异常
     *
     * @return 异常
     */
    public static Throwable normalizeException(Throwable throwable) {
        if (throwable instanceof ExecutionException) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof CompletionException) {
            throwable = throwable.getCause();
        }

        return throwable;
    }
}
