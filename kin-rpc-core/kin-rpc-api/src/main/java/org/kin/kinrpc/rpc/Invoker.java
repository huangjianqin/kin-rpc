package org.kin.kinrpc.rpc;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker<T> extends Node {
    /**
     * invoke
     *
     * @param invocation  相关元数据
     * @return invoke结果
     */
    Result invoke(Invocation invocation);

    /**
     * 获取代理的接口类
     * @return 代理接口类
     */
    Class<T> getInterface();
}
