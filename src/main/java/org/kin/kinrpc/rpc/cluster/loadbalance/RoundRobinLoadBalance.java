package org.kin.kinrpc.rpc.cluster.loadbalance;

import org.apache.log4j.Logger;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RoundRobinLoadBalance implements LoadBalance {
    private static final Logger log = Logger.getLogger(RoundRobinLoadBalance.class);

    private AtomicInteger round = new AtomicInteger(0);

    public ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers) {
        if (invokers != null && invokers.size() > 0) {
            return invokers.get(next(invokers.size()));
        }

        return null;
    }

    private int next(int size) {
        return (round.getAndAdd(1) + size) % size;
    }
}
