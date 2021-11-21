package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于random的负载均衡实现
 * Created by 健勤 on 2017/2/15.
 */
public class RandomLoadBalance implements LoadBalance {
    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        if (CollectionUtils.isNonEmpty(invokers)) {
            if (invokers.size() == 1) {
                return invokers.get(0);
            }

            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        }

        return null;
    }
}
