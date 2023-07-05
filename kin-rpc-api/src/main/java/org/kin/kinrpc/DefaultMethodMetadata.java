package org.kin.kinrpc;

import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.utils.HandlerUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 默认的服务方法元数据, 通过解析{@link Method}得到的
 *
 * @author huangjianqin
 * @date 2023/7/1
 */
public class DefaultMethodMetadata implements MethodMetadata {
    private final Method method;
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
    /**
     * 方法返回类型泛型参数实际类型
     * 非泛型, 则是Object
     */
    private final Class<?> inferredClassForReturn;

    public DefaultMethodMetadata(String service, Method method) {
        this.method = method;
        this.handler = HandlerUtils.handler(service, RpcUtils.getUniqueName(method));
        this.handlerId = HandlerUtils.handlerId(this.handler);
        this.paramsType = method.getParameterTypes();
        this.returnType = method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(returnType) ||
                Publisher.class.isAssignableFrom(returnType)) {
            this.asyncReturn = true;
        } else {
            this.asyncReturn = false;
        }
        this.oneWay = Void.class.equals(returnType);
        this.inferredClassForReturn = ClassUtils.getInferredClassForGeneric(method.getGenericReturnType());
    }

    /**
     * 返回真实返回值
     * 异步返回(比如CompletableFuture, Mono或Flux等等), 则取返回值中的泛型参数
     * 同步返回, 直接取返回值
     *
     * @return 真实返回值
     */
    @Override
    public Class<?> realReturnType() {
        if (asyncReturn) {
            return inferredClassForReturn;
        } else {
            return returnType;
        }
    }

    /**
     * 判断是否是{@link Object}定义方法
     *
     * @return true表示是{@link Object}定义方法
     */
    @Override
    public boolean isObjectMethod() {
        return Object.class.equals(method.getDeclaringClass());
    }

    //getter
    @Override
    public Method method() {
        return method;
    }

    @Override
    public String handlerName() {
        return RpcUtils.getUniqueName(method);
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
    public boolean isAsyncReturn() {
        return asyncReturn;
    }

    @Override
    public boolean isOneWay() {
        return oneWay;
    }

    @Override
    public String toString() {
        return "DefaultMethodMetadata{" +
                "method=" + method +
                ", handlerId=" + handlerId +
                ", handler='" + handler +
                ", paramsType=" + Arrays.toString(paramsType) +
                ", returnType=" + returnType +
                ", asyncReturn=" + asyncReturn +
                ", oneWay=" + oneWay +
                ", inferredClassForReturn=" + inferredClassForReturn +
                '}';
    }
}
