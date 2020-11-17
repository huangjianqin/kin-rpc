package org.kin.kinrpc.rpc;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker<T> extends Node {
    /**
     * rpc调用方法
     *
     * @param methodName 方法名
     * @param params     方法参数
     * @return 返回方法结果(rpc调用)
     * @throws Throwable 异常
     */
    Object invoke(String methodName, Object[] params) throws Throwable;

    /**
     * 获取代理的接口类
     */
    Class<T> getInterface();
}
