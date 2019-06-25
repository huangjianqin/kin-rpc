package org.kin.kinrpc.rpc.cluster;


import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

/**
 * Created by 健勤 on 2017/2/13.
 * 支持容错
 */
public interface Cluster {
    /**
     * 从注册中心中多个invoker中选择一个,包含router, loadbalance
     */
    AbstractReferenceInvoker get();

    void shutdown();
}
