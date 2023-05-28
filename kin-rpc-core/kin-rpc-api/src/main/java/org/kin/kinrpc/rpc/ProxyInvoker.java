package org.kin.kinrpc.rpc;

import org.checkerframework.checker.units.qual.A;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.Proxys;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/2/27
 */
public class ProxyInvoker<T> extends AbstractInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ProxyInvoker.class);

    /** 代理接口 */
    private final Class<T> type;
    /** 代理接口实现类实例 */
    private final T instance;
    /** 生成的代理接口方法invoker */
    private Map<String, org.kin.framework.proxy.ProxyInvoker<?>> methodInvokerMap = new HashMap<>();

    public ProxyInvoker(Url url, Class<T> type, T instance) {
        super(url);
        this.type = type;
        this.instance = instance;
        //生成方法代理类
        init(instance, type);
    }

    private void init(Object service, Class<T> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        Map<String, org.kin.framework.proxy.ProxyInvoker<?>> methodMap = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            if (Object.class.equals(method.getDeclaringClass())) {
                //过滤Object定义的方法
                continue;
            }
            String uniqueName = method.getName();
            org.kin.framework.proxy.ProxyInvoker<?> proxyInvoker = Proxys.adaptive().enhanceMethod(
                    new MethodDefinition<>(service, method));
            methodMap.put(uniqueName, proxyInvoker);
        }
        this.methodInvokerMap = methodMap;
    }

    @Override
    public Result invoke(Invocation invocation) {
        try {
            Object ret = doInvoke(invocation);
            CompletableFuture<Object> retFuture = wrapFuture(ret);
            CompletableFuture<CommonResult> resultFuture = retFuture.handle((obj, t) -> {
                CommonResult commonResult = new CommonResult(invocation);
                if (Objects.nonNull(t)) {
                    commonResult.setException(t);
                } else {
                    commonResult.setValue(obj);
                }

                return commonResult;
            });

            return new AsyncResult(invocation, resultFuture);
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    /**
     * 将invoke结果封装成{@link CompletableFuture}
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
        String methodName = invocation.getMethodName();
        Object[] params = invocation.getParams();

        log.debug("'{}' method '{}' invoking...", url.getServiceKey(), methodName);
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


    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
        //do nothing
    }
}
