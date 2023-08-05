package org.kin.kinrpc;

import org.kin.framework.utils.SPI;

/**
 * rpc call filter
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
@SPI(alias = "filter")
public interface Filter {
    /**
     * rpc call过滤
     *
     * @param invoker    filter chain中下一个{@link Invoker}实例
     * @param invocation 服务调用信息
     * @return 服务调用结果
     */
    RpcResult invoke(Invoker<?> invoker, Invocation invocation);

    /**
     * rpc response时触发
     * user可以通过{@code response}修改服务调用结果
     *
     * @param invocation rpc call信息
     * @param response   rpc response
     */
    default void onResponse(Invocation invocation,
                            RpcResponse response) {
        //default do nothing
    }

    /**
     * 优先级, 数值越大, 优先级越低
     *
     * @return 优先级
     */
    default int order() {
        return Integer.MAX_VALUE;
    }
}
