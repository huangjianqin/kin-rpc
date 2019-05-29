package org.kin.kinrpc.rpc.cluster.loadbalance;

import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RandomLoadBalance implements LoadBalance {
    private static final Logger log = LoggerFactory.getLogger(RandomLoadBalance.class);

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
