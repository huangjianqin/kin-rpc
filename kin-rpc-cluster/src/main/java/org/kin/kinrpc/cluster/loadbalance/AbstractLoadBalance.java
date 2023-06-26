package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.ReferenceInvoker;

/**
 * @author huangjianqin
 * @date 2021/11/21
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    /**
     * 返回invoker权重
     *
     * @param invoker invoker
     * @return invoker权重
     */
    protected int weight(ReferenceInvoker<?> invoker) {
        return invoker.serviceInstance().weight();
    }
}
