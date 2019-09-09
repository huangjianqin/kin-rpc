package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.invoker.JavassistMethodInvoker;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.utils.JavassistUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2019-09-09
 * <p>
 * 调用方法速度接近于直接调用, 比反射要快很多
 */
public class JavassistProviderInvoker extends ProviderInvoker {
    /**
     * 方法调用入口
     */
    private Map<String, JavassistMethodInvoker> methodMap = new HashMap<>();

    public JavassistProviderInvoker(String serviceName, Object service, Class interfaceClass) {
        super(serviceName);
        //生成方法代理类
        init(service, interfaceClass);
    }

    private void init(Object service, Class interfaceClass) {
        methodMap = JavassistUtils.generateProviderMethodProxy(service, interfaceClass, serviceName);
    }

    @Override
    public Object doInvoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        JavassistMethodInvoker methodInvoker = methodMap.get(methodName);

        if (methodInvoker == null) {
            throw new IllegalStateException("service don't has method whoes name is " + methodName);
        }

        //打印日志信息
        int paramLength = params == null ? 0 : params.length;
        String[] actualParamTypeStrs = new String[paramLength];
        for (int i = 0; i < actualParamTypeStrs.length; i++) {
            actualParamTypeStrs[i] = params[i].getClass().getName();
        }
        log.debug("'{}' method's actual params' type", methodName);
        log.debug(StringUtils.mkString(actualParamTypeStrs));

        try {
            return methodInvoker.invoke(params);
        } catch (IllegalAccessException e) {
            log.error("service '{}' method '{}' access illegally", getServiceName(), methodName);
            log.error(e.getMessage(), e);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("service '{}' method '{}' invoke error", getServiceName(), methodName);
            throw e.getCause();
        }
    }
}
