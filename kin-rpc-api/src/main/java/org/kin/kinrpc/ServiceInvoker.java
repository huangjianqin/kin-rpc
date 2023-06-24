package org.kin.kinrpc;

import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.Proxys;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/2/27
 */
public class ServiceInvoker<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ServiceInvoker.class);

    private final ServiceConfig<T> config;
    /** 服务实例 */
    private final T instance;
    /** 服务方法invoker */
    private Map<String, org.kin.framework.proxy.ProxyInvoker<?>> methodInvokerMap = Collections.emptyMap();

    public ServiceInvoker(ServiceConfig<T> config, T instance) {
        this.config = config;
        this.instance = instance;
        //生成方法代理类
        init(instance, config.getInterfaceClass());
    }

    /**
     * 初始化
     */
    private void init(Object service, Class<T> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        Map<String, org.kin.framework.proxy.ProxyInvoker<?>> methodInvokerMap = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            String uniqueName = method.getName();

            if (Object.class.equals(method.getDeclaringClass())) {
                //过滤Object定义的方法
                continue;
            }

            if (!RpcUtils.isRpcMethodValid(method)) {
                log.warn("service method '{}' is ignore, due to it is invalid", uniqueName);
                continue;
            }

            org.kin.framework.proxy.ProxyInvoker<?> proxyInvoker = Proxys.adaptive().enhanceMethod(
                    new MethodDefinition<>(service, method));
            methodInvokerMap.put(uniqueName, proxyInvoker);
        }
        this.methodInvokerMap = methodInvokerMap;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        try {
            Object ret = doInvoke(invocation);
            CompletableFuture<Object> wrappedFuture = wrapFuture(ret);
            return RpcResult.success(invocation, wrappedFuture);
        } catch (Exception e) {
            return RpcResult.fail(invocation, new RpcException(e));
        }
    }

    /**
     * 将服务调用结果封装成{@link CompletableFuture}
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<Object> wrapFuture(Object ret) {
        if (ret instanceof CompletableFuture) {
            return (CompletableFuture<Object>) ret;
        } else {
            return CompletableFuture.completedFuture(ret);
        }
    }

    /**
     * 真正调用方法
     */
    private Object doInvoke(Invocation invocation) {
        String serviceName = invocation.getGsv();
        String methodName = invocation.getMethodName();
        Object[] params = invocation.getParams();

        log.debug("'{}' method '{}' invoking...", serviceName, methodName);
        //Object类方法直接调用
        if ("getClass".equals(methodName)) {
            return instance.getClass();
        }
        if ("hashCode".equals(methodName)) {
            return instance.hashCode();
        }
        if ("toString".equals(methodName)) {
            return instance.toString();
        }
        if ("equals".equals(methodName)) {
            if (params.length == 1) {
                return instance.equals(params[0]);
            }
            throw new IllegalArgumentException(String.format("method '%s' parameter number error", methodName));
        }

        org.kin.framework.proxy.ProxyInvoker<?> methodInvoker = methodInvokerMap.get(methodName);

        if (methodInvoker == null) {
            throw new IllegalStateException("cannot find invoker which method name is " + methodName);
        }

        //打印入参信息
        int paramLength = params == null ? 0 : params.length;
        String[] actualParamTypeNames = new String[paramLength];
        for (int i = 0; i < actualParamTypeNames.length; i++) {
            actualParamTypeNames[i] = params[i].getClass().getName();
        }
        log.debug("method '{}' actual params' type is {}", methodName, actualParamTypeNames);

        try {
            return methodInvoker.invoke(params);
        } catch (Exception e) {
            log.error("method '{}' invoke error, params is {}, {}", methodName, params, e);
            throw new RpcException(e);
        }
    }

    //getter
    public ServiceConfig<T> getConfig() {
        return config;
    }

    public Class<T> getInterface() {
        return config.getInterfaceClass();
    }
}
