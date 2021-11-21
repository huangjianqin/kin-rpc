package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RoundRobinLoadBalance implements LoadBalance {
    private AtomicInteger round = new AtomicInteger(0);

    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        if (CollectionUtils.isNonEmpty(invokers)) {
            return invokers.get(next(invokers.size()));
        }

        return null;
    }

    private int next(int size) {
        if (round.get() <= 0) {
            round.set(0);
        }
        return (round.getAndAdd(1) + size) % size;
    }
}
