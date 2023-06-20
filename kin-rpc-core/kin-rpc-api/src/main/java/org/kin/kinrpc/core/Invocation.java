package org.kin.kinrpc.core;

/**
 * {@link Invoker#invoke(Invocation)}参数
 * .invoke相关元数据
 *
 * @author huangjianqin
 * @date 2023/2/26
 */
public interface Invocation {
    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    int getServiceId();

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    String getGsv();

    /**
     * 返回服务方法名
     *
     * @return 服务方法名
     */
    String getMethodName();

    /**
     * 返回服务调用参数
     *
     * @return 服务调用参数
     */
    Object[] getParams();

    /**
     * 返回服务调用结果是否异步返回
     *
     * @return true表示服务调用结果是异步返回
     */
    boolean isAsyncReturn();

    /**
     * 返回服务调用结果真实返回值
     * 异步返回(比如CompletableFuture, Mono或Flux等等), 则取返回值中的泛型参数
     * 同步返回, 直接取返回值
     *
     * @return 服务调用结果真实返回值
     */
    Class<?> getRealReturnType();

    /**
     * 返回服务方法返回值
     *
     * @return 服务方法返回值
     */
    Class<?> getReturnType();
}
