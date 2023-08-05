package org.kin.kinrpc;

import org.kin.framework.collection.AttachmentSupport;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link Invoker#invoke(Invocation)}参数
 * 服务调用上下文信息
 *
 * @author huangjianqin
 * @date 2023/2/26
 */
public interface Invocation extends AttachmentSupport {
    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id
     */
    int serviceId();

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    String service();

    /**
     * 返回服务名
     *
     * @return 服务名
     */
    String serviceName();

    /**
     * 返回服务接口类
     *
     * @return 服务接口类
     */
    Class<?> interfaceClass();

    /**
     * 返回服务方法唯一id
     *
     * @return 服务方法唯一id
     */
    default int handlerId() {
        return methodMetadata().handlerId();
    }

    /**
     * 返回服务方法唯一标识
     *
     * @return 服务方法唯一标识
     */
    default String handler() {
        return methodMetadata().handler();
    }

    /**
     * 返回服务方法名
     *
     * @return 服务方法名
     */
    default String handlerName() {
        return methodMetadata().handlerName();
    }

    /**
     * 返回服务调用参数
     *
     * @return 服务调用参数
     */
    Object[] params();

    /**
     * 返回服务调用结果是否异步返回
     *
     * @return true表示服务调用结果是异步返回
     */
    default boolean isAsyncReturn() {
        return methodMetadata().isAsyncReturn();
    }

    /** 标识服务方法没有返回值 */
    default boolean isOneWay() {
        return methodMetadata().isOneWay();
    }

    /**
     * 返回服务调用结果真实返回值
     * 异步返回(比如CompletableFuture, Mono或Flux等等), 则取返回值中的泛型参数
     * 同步返回, 直接取返回值
     *
     * @return 服务调用结果真实返回值
     */
    default Class<?> realReturnType() {
        return methodMetadata().realReturnType();
    }

    /**
     * 返回服务方法返回值
     *
     * @return 服务方法返回值
     */
    default Class<?> returnType() {
        return methodMetadata().returnType();
    }

    /**
     * 判断是否是{@link Object}定义方法
     *
     * @return true表示是{@link Object}定义方法
     */
    default boolean isObjectMethod() {
        return methodMetadata().isObjectMethod();
    }

    /**
     * 返回值是否为空
     *
     * @return true表示返回值为空
     */
    default boolean isVoid() {
        return Void.class.equals(returnType());
    }

    /**
     * 返回发送给server的attachments
     *
     * @return attachments
     */
    Map<String, String> serverAttachments();

    /**
     * 返回接口方法实例
     *
     * @return 接口方法实例
     */
    default Method method() {
        return methodMetadata().method();
    }

    /**
     * 返回{@link MethodMetadata}实例
     *
     * @return {@link MethodMetadata}实例
     */
    MethodMetadata methodMetadata();
}
