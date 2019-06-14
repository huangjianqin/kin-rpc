package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.kin.kinrpc.rpc.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class SimpleInvoker implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    private final AsyncInvoker invoker;

    public SimpleInvoker(AsyncInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");

        //异步方式: consumer端必须自定义一个与service端除了返回值为Future.class或者CompletableFuture.class外,
        //方法签名相同的接口
        Class returnType = method.getReturnType();
        if (Future.class.equals(returnType)) {
            return invoker.invokeAsync(ClassUtils.getUniqueName(method), args);
        } else if (CompletableFuture.class.equals(returnType)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return invoker.invokeAsync(ClassUtils.getUniqueName(method), args).get();
                } catch (Exception e) {
                    ExceptionUtils.log(e);
                }

                return null;
            });
        }

        return invoker.invoke(ClassUtils.getUniqueName(method), Void.class.equals(returnType), args);
    }
}
