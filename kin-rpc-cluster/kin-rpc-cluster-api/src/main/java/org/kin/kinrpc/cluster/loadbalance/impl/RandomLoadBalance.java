package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.framework.utils.CollectionUtils;
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
        if (CollectionUtils.isNonEmpty(invokers)) {
            if (invokers.size() == 1) {
                return invokers.get(0);
            }

            Random random = new Random();
            return invokers.get(random.nextInt(invokers.size()));
        }

        return null;
    }
}
