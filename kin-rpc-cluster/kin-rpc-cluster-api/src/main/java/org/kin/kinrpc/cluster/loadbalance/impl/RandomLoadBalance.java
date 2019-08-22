package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;
import java.util.Random;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RandomLoadBalance implements LoadBalance {
    @Override
    public ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers) {
        if (invokers != null && invokers.size() > 0) {
            if (invokers.size() == 1) {
                return invokers.get(0);
            }

            Random random = new Random(invokers.size());
            return invokers.get(random.nextInt());
        }

        return null;
    }
}
