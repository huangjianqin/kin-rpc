package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class JavaProviderInvoker extends ProviderInvoker {
    private static final Logger log = LoggerFactory.getLogger(JavaProviderInvoker.class);

    public JavaProviderInvoker(Object serivce, Class<?> interfaceClass) {
        super(interfaceClass);
        this.serivce = serivce;
        init();
    }

    private void init() {
        if (this.interfaceClass == null || this.serivce == null) {
            throw new IllegalStateException("service class and its interface class must not null");
        }

        if (!this.interfaceClass.isInterface()) {
            throw new IllegalStateException("the class '" + this.interfaceClass.getName() + "' is not a interface");
        }

        Class<?> serviceClass = serivce.getClass();
        if (!this.interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalStateException("service class " + serviceClass.getName() + " must implements the certain interface '" + this.interfaceClass.getName() + "'");
        }
        log.info("initing service '" + getServiceName() + "'");

        Method[] methods = this.interfaceClass.getMethods();

        for (Method method : methods) {
            method.setAccessible(true);
            String methodName = method.getName();
            this.methodMap.put(methodName, method);
        }
    }

    public Object invoke(String methodName, Object... params) {
        log.info("service '" + getServiceName() + "' method '" + methodName + "' invoking...");
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
        log.info("'" + methodName + "' method's params' type");
        log.info(StringUtils.mkString(",", paramTypeStrs));

        String[] actualParamTypeStrs = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypeStrs[i] = params[i].getClass().getName();
        }
        log.info("'" + methodName + "' method's actual params' type");
        log.info(StringUtils.mkString(",", actualParamTypeStrs));

        try {
            return target.invoke(serivce, params);
        } catch (IllegalAccessException e) {
            log.error("service '" + getServiceName() + "' method '" + methodName + "' access illegally");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            log.error("service '" + getServiceName() + "' method '" + methodName + "' invoke error");
            e.printStackTrace();
        }

        return null;
    }


}
