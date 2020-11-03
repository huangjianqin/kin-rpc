package org.kin.kinrpc.cluster.loadbalance;


import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
@SPI(value = "hash", key = "loadbalance")
public interface LoadBalance {
    /**
     * 负载均衡 过滤invokers
     *
     * @param invokers 可用invokers
     * @return 过滤后的invoker
     */
    ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers);
}
