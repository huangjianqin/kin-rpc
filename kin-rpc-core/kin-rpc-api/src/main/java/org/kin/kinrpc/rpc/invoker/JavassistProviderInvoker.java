package org.kin.kinrpc.rpc.invoker;

import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Url;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2019-09-09
 * <p>
 * 调用方法速度接近于直接调用, 比反射要快很多
 */
public class JavassistProviderInvoker<T> extends ProviderInvoker<T> {
    /**
     * 方法调用入口
     */
    private Map<String, ProxyInvoker> methodMap = new HashMap<>();

    public JavassistProviderInvoker(Url url, T service, Class<T> interfaceClass) {
        super(url, interfaceClass);
        //生成方法代理类
        init(service, interfaceClass);
    }

    private void init(Object service, Class<T> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        Map<String, ProxyInvoker> methodMap = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            String uniqueName = ClassUtils.getUniqueName(method);
            ProxyInvoker proxyInvoker = ProxyEnhanceUtils.enhanceMethod(
                    new ProxyMethodDefinition(service, method,
                            "org.kin.kinrpc.rpc.invoker.proxy", interfaceClass.getSimpleName() + "$" + ClassUtils.getUniqueName(method)));
            methodMap.put(uniqueName, proxyInvoker);
        }
        this.methodMap = methodMap;
    }

    @Override
    public Object doInvoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        ProxyInvoker methodInvoker = methodMap.get(methodName);

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
