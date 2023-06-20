package org.kin.kinrpc.core;

import org.kin.framework.utils.ClassUtils;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * 服务方法元数据
 * todo     目前还不能支持重载, 是否需要支持自定义rpc method name
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class MethodMetadata {
    /** 方法名 */
    private final String name;
    /** 服务防范参数类型 */
    private final Class<?>[] paramsType;
    /** 方法返回类型 */
    private final Class<?> returnType;
    /** 标识是否是异步返回 */
    private final boolean asyncReturn;
    /**
     * 方法返回类型泛型参数实际类型
     * 非泛型, 则是Object
     */
    private final Class<?> inferredClassForReturn;

    public MethodMetadata(Method method) {
        this.name = method.getName();
        this.paramsType = method.getParameterTypes();
        this.returnType = method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(returnType) ||
                Publisher.class.isAssignableFrom(returnType)) {
            asyncReturn = true;
        } else {
            asyncReturn = false;
        }
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

    //getter
    public String getName() {
        return name;
    }

    public Class<?>[] getParamsType() {
        return paramsType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public boolean isAsyncReturn() {
        return asyncReturn;
    }
}
