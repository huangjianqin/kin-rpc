package org.kin.kinrpc;

import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.utils.HandlerUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 服务方法元数据
 * todo     目前还不能支持重载, 是否需要支持自定义rpc method name
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class MethodMetadata {
    private final Method method;
    /** 服务方法唯一id */
    private final int handlerId;
    /** 服务方法唯一标识 */
    private final String handler;
    /** 服务防范参数类型 */
    private final Class<?>[] paramsType;
    /** 方法返回类型 */
    private final Class<?> returnType;
    /** 标识是否是异步返回 */
    private final boolean asyncReturn;
    /** 标识服务方法没有返回值 */
    private final boolean oneWay;
    /**
     * 方法返回类型泛型参数实际类型
     * 非泛型, 则是Object
     */
    private final Class<?> inferredClassForReturn;

    public MethodMetadata(String service, Method method) {
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
    public Class<?> getRealReturnType() {
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
    public boolean isObjectMethod() {
        return Object.class.equals(method.getDeclaringClass());
    }

    //getter
    public Method method() {
        return method;
    }

    public String methodName() {
        return RpcUtils.getUniqueName(method);
    }

    public int handlerId() {
        return handlerId;
    }

    public String handler() {
        return handler;
    }

    public Class<?>[] paramsType() {
        return paramsType;
    }

    public Class<?> returnType() {
        return returnType;
    }

    public boolean isAsyncReturn() {
        return asyncReturn;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    @Override
    public String toString() {
        return "MethodMetadata{" +
                "method=" + method +
                ", handlerId=" + handlerId +
                ", handler='" + handler + '\'' +
                ", paramsType=" + Arrays.toString(paramsType) +
                ", returnType=" + returnType +
                ", asyncReturn=" + asyncReturn +
                ", oneWay=" + oneWay +
                ", inferredClassForReturn=" + inferredClassForReturn +
                '}';
    }
}
