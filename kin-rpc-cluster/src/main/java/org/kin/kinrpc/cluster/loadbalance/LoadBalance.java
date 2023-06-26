package org.kin.kinrpc.cluster.loadbalance;


import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/6/25
 */
@SPI(alias = "loadbalance")
public interface LoadBalance {
    /**
     * 应用负载均衡策略过滤invokers
     *
     * @param invokers 可用invokers
     * @return selected invoker
     */
    ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers);
}
