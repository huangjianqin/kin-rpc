package org.kin.kinrpc;

import org.kin.kinrpc.utils.HandlerUtils;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/1
 */
public class GenericMethodMetadata implements MethodMetadata {
    /** 泛化方法 */
    private final Method genericMethod;
    /** 服务方法名 */
    private final String handlerName;
    /** 服务方法唯一id */
    private final int handlerId;
    /** 服务方法唯一标识 */
    private final String handler;
    /** 服务方法参数类型 */
    private final Class<?>[] paramsType;
    /** 服务方法返回类型 */
    private final Class<?> returnType;
    /** 标识是否是异步返回 */
    private final boolean asyncReturn;
    /** 标识服务方法返回值是void */
    private final boolean oneWay;
    /** 服务方法真实返回值 */
    private final Class<?> realReturnType;
    /** 标识是否是{@link Object}定义方法 */
    private final boolean objectMethod;

    public GenericMethodMetadata(String service,
                                 Method genericMethod,
                                 String handlerName,
                                 Object[] args,
                                 Class<?> returnType) {
        this.genericMethod = genericMethod;
        this.handlerName = handlerName;
        this.handler = HandlerUtils.handler(service, handlerName);
        this.handlerId = HandlerUtils.handlerId(this.handler);
        Class<?>[] paramsType = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramsType[i] = args[i].getClass();
        }
        this.paramsType = paramsType;
        Class<?> genericReturnType = genericMethod.getReturnType();
        this.returnType = genericReturnType;
        if (CompletableFuture.class.isAssignableFrom(genericReturnType) ||
                Publisher.class.isAssignableFrom(genericReturnType)) {
            this.asyncReturn = true;
        } else {
            this.asyncReturn = false;
        }
        this.oneWay = Void.class.equals(genericReturnType);
        this.realReturnType = returnType;

        boolean isObjectMethod = false;
        for (Method objectMethod : Object.class.getDeclaredMethods()) {
            if (objectMethod.getName().equals(handlerName)) {
                isObjectMethod = true;
                break;
            }
        }
        this.objectMethod = isObjectMethod;
    }

    @Override
    public Method method() {
        throw new UnsupportedOperationException("generic method is not real rpc service method");
    }

    @Override
    public String handlerName() {
        return handlerName;
    }

    @Override
    public int handlerId() {
        return handlerId;
    }

    @Override
    public String handler() {
        return handler;
    }

    @Override
    public Class<?>[] paramsType() {
        return paramsType;
    }

    @Override
    public Class<?> returnType() {
        return returnType;
    }

    @Override
    public Class<?> realReturnType() {
        return realReturnType;
    }

    @Override
    public boolean isObjectMethod() {
        return objectMethod;
    }

    @Override
    public boolean isAsyncReturn() {
        return asyncReturn;
    }

    @Override
    public boolean isOneWay() {
        return oneWay;
    }
}
