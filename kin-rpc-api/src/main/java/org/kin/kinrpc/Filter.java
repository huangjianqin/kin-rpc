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
    /** 最高优先级 */
    int HIGHEST_ORDER = -10000;
    /** 高优先级5 */
    int HIGH_ORDER_5 = -9000;
    /** 高优先级4 */
    int HIGH_ORDER_4 = -8000;
    /** 高优先级3 */
    int HIGH_ORDER_3 = -7000;
    /** 高优先级2 */
    int HIGH_ORDER_2 = -6000;
    /** 高优先级1 */
    int HIGH_ORDER_1 = -5000;
    /** 普通优先级 */
    int COMMON_ORDER = 0;
    /** 低优先级1 */
    int LOW_ORDER_1 = 5000;
    /** 低优先级2 */
    int LOW_ORDER_2 = 6000;
    /** 低优先级3 */
    int LOW_ORDER_3 = 7000;
    /** 低优先级4 */
    int LOW_ORDER_4 = 8000;
    /** 低优先级5 */
    int LOW_ORDER_5 = 9000;
    /** 最低优先级 */
    int LOWEST_ORDER = 10000;

    /**
     * rpc call过滤
     *
     * @param invoker    filter chain中下一个{@link Invoker}实例
     * @param invocation 服务调用信息
     * @return 服务调用结果
     */
    RpcResult invoke(Invoker<?> invoker, Invocation invocation);

    /**
     * call after remoting rpc response
     * <p>
     * user可以通过{@code response}修改服务调用结果
     * 甚至可以通过{@link RpcResponse#setException(Throwable)}设置rpc call异常, 哪怕rpc call正常返回, 最后user也收到异常
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
        return LOWEST_ORDER;
    }
}
