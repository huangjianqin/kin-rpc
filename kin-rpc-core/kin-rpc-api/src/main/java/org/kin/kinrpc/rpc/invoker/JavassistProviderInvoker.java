package org.kin.kinrpc.rpc.invoker;

import org.kin.framework.proxy.Javassists;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.Proxys;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Url;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 效率比反射要高
 *
 * @author huangjianqin
 * @date 2019-09-09
 */
public class JavassistProviderInvoker<T> extends ProviderInvoker<T> {
    /**
     * 方法调用入口
     */
    private Map<String, ProxyInvoker<?>> methodMap = new HashMap<>();

    public JavassistProviderInvoker(Url url, T service, Class<T> interfaceClass) {
        super(url, interfaceClass, service);
        //生成方法代理类
        init(service, interfaceClass);
    }

    private void init(Object service, Class<T> interfaceClass) {
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        Map<String, ProxyInvoker<?>> methodMap = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            String uniqueName = method.getName();
            ProxyInvoker<?> proxyInvoker = Proxys.javassist().enhanceMethod(
                    new MethodDefinition<>(service, method));
            methodMap.put(uniqueName, proxyInvoker);

            log.info("service '{}'s method '{}'/'{}' is ready to provide service", url.getServiceKey(), uniqueName, method.toString());
        }
        this.methodMap = methodMap;
    }

    @Override
    public Object doInvoke(String methodName, Object... params) throws Throwable {
        ProxyInvoker<?> methodInvoker = methodMap.get(methodName);

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
            log.error("service '{}' method '{}' access illegally", url.getServiceKey(), methodName);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("service '{}' method '{}' invoke error", url.getServiceKey(), methodName);
            throw e.getCause();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (ProxyInvoker<?> proxyInvoker : methodMap.values()) {
            //释放无用代理类
            Javassists.detach(proxyInvoker.getClass().getName());
        }
    }
}
