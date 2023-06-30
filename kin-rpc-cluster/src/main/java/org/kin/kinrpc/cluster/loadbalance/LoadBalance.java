package org.kin.kinrpc.cluster.loadbalance;


import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;

/**
 * todo 增加基于性能监控动态计算权重的负载均衡算法
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
@SPI("loadbalance")
public interface LoadBalance {
    /**
     * 应用负载均衡策略过滤invokers
     *
     * @param invokers 可用invokers
     * @return selected invoker
     */
    ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers);
}
