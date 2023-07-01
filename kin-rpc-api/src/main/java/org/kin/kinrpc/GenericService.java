package org.kin.kinrpc;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * 泛化服务
 *
 * @author huangjianqin
 * @date 2020/11/16
 */
public interface GenericService {
    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     * @param returnType  服务方法返回值
     * @return rpc call result
     */
    Object invoke(String handlerName, Object[] args, Class<?> returnType);

    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     */
    void invoke(String handlerName, Object[] args);

    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     * @param returnType  服务方法返回值
     * @return rpc call result future
     */
    CompletableFuture<Object> asyncInvoke(String handlerName, Object[] args, Class<?> returnType);

    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     * @return rpc call result future
     */
    CompletableFuture<Void> asyncInvoke(String handlerName, Object[] args);

    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     * @param returnType  服务方法返回值
     * @return rpc call result mono
     */
    Mono<Object> reactiveInvoke(String handlerName, Object[] args, Class<?> returnType);

    /**
     * 泛化调用
     *
     * @param handlerName 服务方法名
     * @param args        服务方法参数
     * @return rpc call result mono
     */
    Mono<Void> reactiveInvoke(String handlerName, Object[] args);
}
