package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * @author huangjianqin
 * @date 2021/11/21
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * 获取负载均衡的粒度key, 默认是以服务方法为key, 不同服务方法的各自负载均衡
     */
    protected int key(String serviceKey, String method) {
        return (serviceKey + "#" + method).hashCode();
    }

    /**
     * 获取{@link AsyncInvoker}权重
     */
    @SuppressWarnings("rawtypes")
    protected int weight(AsyncInvoker invoker) {
        return invoker.url().getIntParam(Constants.WEIGHT, Constants.DEFAULT_WEIGHT);
    }
}
