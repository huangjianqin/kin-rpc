package org.kin.kinrpc.fallback;

import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcExceptionBlockException;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.constants.InvocationConstants;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/8/3
 */
public final class FallbackManager {
    private FallbackManager() {
    }

    /** key -> fallback (service) class, value -> fallback instance */
    private static final Map<Class<?>, Object> FALLBACK_INST_MAP = new CopyOnWriteMap<>();

    /**
     * 调用{@link Fallback}实例
     *
     * @param invocation    rpc call info
     * @param fallbackClass fallback class
     * @return 服务降级result
     */
    private static RpcResult invokeFallback(Invocation invocation,
                                            RpcExceptionBlockException blockException,
                                            Class<?> fallbackClass) {
        Fallback fallbackInstance = (Fallback) FALLBACK_INST_MAP.computeIfAbsent(fallbackClass, c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                ExceptionUtils.throwExt(e);
                return null;
            }
        });

        assert fallbackInstance != null;
        return fallbackInstance.handle(invocation, blockException);
    }

    /**
     * invoke fallback service
     *
     * @param invocation           rpc call info
     * @param fallbackServiceClass fallback service
     * @return 服务降级result
     */
    private static RpcResult invokeFallbackService(Invocation invocation, Class<?> fallbackServiceClass) {
        Object fallbackServiceInstance = FALLBACK_INST_MAP.computeIfAbsent(fallbackServiceClass, c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                ExceptionUtils.throwExt(e);
                return null;
            }
        });

        assert fallbackServiceInstance != null;
        try {
            Object result = invocation.method().invoke(fallbackServiceInstance, invocation.params());
            return RpcResult.success(invocation,
                    CompletableFuture.completedFuture(result));
        } catch (Exception e) {
            return RpcResult.fail(invocation, e);
        }
    }

    /**
     * 服务降级, 触发fallback
     *
     * @param invocation rpc call info
     * @return 服务降级result
     */
    public static RpcResult onFallback(Invocation invocation, RpcExceptionBlockException blockException) {
        String fallbackName = invocation.attachment(InvocationConstants.FALLBACK_KEY);
        if (StringUtils.isBlank(fallbackName)) {
            return onFallbackNotFound(invocation);
        }

        try {
            if (fallbackName.toLowerCase().equals(Boolean.TRUE.toString())) {
                //同classpath下找名为{service interface}Fallback的类
                RpcResult fallbackResult = invokeFallbackService(invocation, ClassUtils.getClass(invocation.interfaceClass().getName() + "Fallback"));
                if (Objects.nonNull(fallbackResult)) {
                    return fallbackResult;
                }
            }

            Class<?> fallbackClass = ClassUtils.getClass(fallbackName);
            if (Fallback.class.isAssignableFrom(fallbackClass)) {
                return invokeFallback(invocation, blockException, fallbackClass);
            } else {
                return invokeFallbackService(invocation, fallbackClass);
            }
        } catch (Exception e) {
            return RpcResult.fail(invocation, new RpcExceptionBlockException(String.format("invoke fallback fail on '%s' service", invocation.service())));
        }
    }

    private static RpcResult onFallbackNotFound(Invocation invocation) {
        return RpcResult.fail(invocation, new RpcExceptionBlockException(String.format("can not find any fallback on '%s' service", invocation.service())));
    }
}
