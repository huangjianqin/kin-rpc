package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.invoker.AbstractProviderInvoker;
import org.kin.kinrpc.rpc.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class JavaProviderInvoker extends AbstractProviderInvoker {
    public JavaProviderInvoker(String serviceName, Object service) {
        super(serviceName);
        this.serivce = service;
    }

    public void init(Class interfaceClass) {
        Method[] methods = interfaceClass.getMethods();

        for (Method method : methods) {
            method.setAccessible(true);
            this.methodMap.put(ClassUtils.getUniqueName(method), method);
        }

        log.info("service '" + getServiceName() + "' is ready to provide service");
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Exception {
        log.debug("service '" + getServiceName() + "' method '" + methodName + "' invoking...");
        Method target = methodMap.get(methodName);

        if (target == null) {
            throw new IllegalStateException("service don't has method whoes name is " + methodName);
        }

        //打印日志信息
        Class<?>[] paramTypes = target.getParameterTypes();
        String[] paramTypeStrs = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeStrs[i] = paramTypes[i].getName();
        }
        log.debug("'" + methodName + "' method's params' type");
        log.debug(StringUtils.mkString(",", paramTypeStrs));

        String[] actualParamTypeStrs = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypeStrs[i] = params[i].getClass().getName();
        }
        log.debug("'" + methodName + "' method's actual params' type");
        log.debug(StringUtils.mkString(",", actualParamTypeStrs));

        try {
            return target.invoke(serivce, params);
        } catch (IllegalAccessException e) {
            log.error("service '" + getServiceName() + "' method '" + methodName + "' access illegally");
            log.error("", e);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("service '" + getServiceName() + "' method '" + methodName + "' invoke error");
            throw e;
        }
    }


}
