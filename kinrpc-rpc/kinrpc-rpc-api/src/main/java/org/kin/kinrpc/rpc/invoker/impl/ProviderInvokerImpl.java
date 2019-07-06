package org.kin.kinrpc.rpc.invoker.impl;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.AbstractProviderInvoker;
import org.kin.kinrpc.rpc.utils.ClassUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ProviderInvokerImpl extends AbstractProviderInvoker {
    private RateLimiter rateLimiter = RateLimiter.create(Constants.PROVIDER_REQUEST_THRESHOLD);

    public ProviderInvokerImpl(String serviceName, Object service) {
        super(serviceName);
        this.serivce = service;
    }

    public void init(Class interfaceClass) {
        Method[] methods = interfaceClass.getMethods();

        for (Method method : methods) {
            method.setAccessible(true);
            String uniqueName = ClassUtil.getUniqueName(method);
            this.methodMap.put(uniqueName, method);
            log.info("service '{}'s method '{}'/'{}' is ready to provide service", getServiceName(), uniqueName, method.toString());
        }
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        log.debug("service '{}' method '{}' invoking...", getServiceName(), methodName);
        //限流
        if(!rateLimiter.tryAcquire()){
            //抛异常, 外部捕获异常并立即返回给reference, 让其重试
            throw new RateLimitException();
        }

        Method target = methodMap.get(methodName);

        if (target == null) {
            throw new IllegalStateException("service don't has method whoes name is " + methodName);
        }

        //打印日志信息
        Class<?>[] paramTypes = target.getParameterTypes();
        String[] paramTypeStrs = new String[paramTypes.length];
        for (int i = 0; i < paramTypeStrs.length; i++) {
            paramTypeStrs[i] = paramTypes[i].getName();
        }
        log.debug("'{}' method's params' type", methodName);
        log.debug(StringUtils.mkString(paramTypeStrs));

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
            log.error("service '{}' method '{}' access illegally", getServiceName(), methodName);
            log.error(e.getMessage(), e);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("service '{}' method '{}' invoke error", getServiceName(), methodName);
            throw e.getCause();
        }
    }


    @Override
    public void setRate(double rate) {
        rateLimiter.setRate(rate);
    }

    @Override
    public double getRate() {
        return rateLimiter.getRate();
    }
}
