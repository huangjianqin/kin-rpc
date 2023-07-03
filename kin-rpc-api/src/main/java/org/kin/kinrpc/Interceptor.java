package org.kin.kinrpc;

/**
 * 服务调用拦截器, client和server共用
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface Interceptor {
    /**
     * 拦截rpc call
     *
     * @param invoker    拦截器调用链中下一个{@link Invoker}实例
     * @param invocation 服务调用信息
     * @return 服务调用结果
     */
    RpcResult intercept(Invoker<?> invoker, Invocation invocation);

    /**
     * rpc response时触发
     *
     * @param invocation rpc call信息
     * @param response   rpc response
     */
    default void onResponse(Invocation invocation,
                            RpcResponse response) {
        //default do nothing
    }

    /**
     * 拦截优先级, 数值越大, 优先级越低
     *
     * @return 优先级
     */
    default int order() {
        return Integer.MAX_VALUE;
    }
}
