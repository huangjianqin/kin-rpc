package org.kin.kinrpc;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.Proxys;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.HandlerUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * service端{@link Invoker}实现
 *
 * @author huangjianqin
 * @date 2023/2/27
 */
public class ServiceInvoker<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ServiceInvoker.class);

    private final ServiceConfig<T> config;
    /** 服务实例 */
    private final T instance;
    /** 服务方法invoker */
    private IntObjectMap<ProxyInvoker<?>> methodInvokerMap;

    public ServiceInvoker(ServiceConfig<T> config, T instance) {
        this.config = config;
        this.instance = instance;

        //生成方法代理类
        init(instance, config.service(), config.getInterfaceClass());
    }

    /**
     * 初始化
     */
    private void init(Object instance,
                      String service,
                      Class<T> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        IntObjectHashMap<ProxyInvoker<?>> methodInvokerMap = new IntObjectHashMap<>(declaredMethods.length);

        for (Method method : declaredMethods) {
            if (!RpcUtils.isRpcMethodValid(method)) {
                continue;
            }

            org.kin.framework.proxy.ProxyInvoker<?> proxyInvoker = Proxys.adaptive().enhanceMethod(
                    new MethodDefinition<>(instance, method));
            methodInvokerMap.put(HandlerUtils.handlerId(service, RpcUtils.getUniqueName(method)), proxyInvoker);
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
        } else if (ret instanceof Mono) {
            return (CompletableFuture<Object>) ((Mono<?>) ret).toFuture();
        } else {
            return CompletableFuture.completedFuture(ret);
        }
    }

    /**
     * 真正调用方法
     */
    private Object doInvoke(Invocation invocation) {
        String methodName = invocation.getMethodName();
        Object[] params = invocation.params();

        if (log.isDebugEnabled()) {
            log.debug("handle rpc call... invocation={}", invocation);
        }
        //Object类方法直接调用
        if (invocation.isObjectMethod()) {
            if ("getClass".equals(methodName)) {
                return instance.getClass();
            } else if ("hashCode".equals(methodName)) {
                return instance.hashCode();
            } else if ("toString".equals(methodName)) {
                return instance.toString();
            } else if ("equals".equals(methodName)) {
                if (params.length == 1) {
                    return instance.equals(params[0]);
                }
                throw new IllegalArgumentException(String.format("method '%s' parameter number error", methodName));
            } else {
                throw new UnsupportedOperationException(String.format("does not support to call method '%s'", methodName));
            }
        }

        //其他方法
        org.kin.framework.proxy.ProxyInvoker<?> methodInvoker = methodInvokerMap.get(invocation.handlerId());

        if (methodInvoker == null) {
            throw new IllegalStateException("cannot find invoker which method name is " + methodName);
        }

        //打印入参信息
        int paramLength = params == null ? 0 : params.length;
        String[] actualParamTypeNames = new String[paramLength];
        for (int i = 0; i < actualParamTypeNames.length; i++) {
            actualParamTypeNames[i] = params[i].getClass().getName();
        }
        if (log.isDebugEnabled()) {
            log.debug("method '{}' actual params' type is {}", methodName, actualParamTypeNames);
        }

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
