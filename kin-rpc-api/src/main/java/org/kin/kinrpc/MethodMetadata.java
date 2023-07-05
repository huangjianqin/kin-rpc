package org.kin.kinrpc;

import java.lang.reflect.Method;

/**
 * 服务方法元数据
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface MethodMetadata {
    /**
     * 返回{@link Method}实例
     *
     * @return {@link Method}实例
     */
    Method method();

    /**
     * 返回方法名
     *
     * @return 方法名
     */
    String handlerName();

    /**
     * 返回服务方法唯一id
     *
     * @return 服务方法唯一id
     */
    int handlerId();

    /**
     * 返回服务方法唯一标识
     *
     * @return 服务方法唯一标识
     */
    String handler();

    /**
     * 返回服务方法参数类型
     *
     * @return 服务方法参数类型
     */
    Class<?>[] paramsType();

    /**
     * 返回服务方法返回值
     *
     * @return 服务方法返回值
     */
    Class<?> returnType();

    /**
     * 返回服务方法真实返回值
     * 异步返回(比如CompletableFuture或Mono等等), 则取返回值中的泛型参数
     * 同步返回, 直接取返回值
     *
     * @return 真实返回值
     */
    Class<?> realReturnType();

    /**
     * 判断是否是{@link Object}定义方法
     *
     * @return true表示是{@link Object}定义方法
     */
    boolean isObjectMethod();

    /**
     * 判断服务方法是否异步返回
     *
     * @return true表示服务方法是异步返回
     */
    boolean isAsyncReturn();

    /**
     * 判断服务方法返回是void
     *
     * @return true表示服务方法返回是void
     */
    boolean isOneWay();
}
