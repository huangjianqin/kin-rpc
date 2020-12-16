package org.kin.kinrpc.rpc.invoker;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Url;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class JdkProxyProviderInvoker<T> extends ProviderInvoker<T> {
    /**
     * 方法调用入口
     */
    private Map<String, Method> methodMap = new HashMap<>();

    public JdkProxyProviderInvoker(Url url, T service, Class<T> interfaceClass) {
        super(url, interfaceClass, service);
        init(interfaceClass);
    }

    private void init(Class<T> interfaceClass) {
        Method[] methods = interfaceClass.getMethods();

        for (Method method : methods) {
            String uniqueName = ClassUtils.getUniqueName(method);

            //打印日志信息
            Class<?>[] paramTypes = method.getParameterTypes();
            String[] paramTypeStrs = new String[paramTypes.length];
            for (int i = 0; i < paramTypeStrs.length; i++) {
                paramTypeStrs[i] = paramTypes[i].getName();
            }
            log.debug("'{}' method's params' type", uniqueName);
            log.debug(StringUtils.mkString(paramTypeStrs));

            method.setAccessible(true);
            this.methodMap.put(uniqueName, method);
            log.info("service '{}'s method '{}'/'{}' is ready to provide service", getServiceKey(), uniqueName, method.toString());
        }
    }

    @Override
    public Object doInvoke(String methodName, Object... params) throws Throwable {
        Method target = methodMap.get(methodName);

        if (target == null) {
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
            return target.invoke(serivce, params);
        } catch (IllegalAccessException e) {
            log.error("service '{}' method '{}' access illegally", getServiceKey(), methodName);
            log.error(e.getMessage(), e);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("service '{}' method '{}' invoke error", getServiceKey(), methodName);
            throw e.getCause();
        }
    }
}
