package org.kinrpc.rpc.cluster.loadbalance;

import org.apache.log4j.Logger;
import org.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.List;
import java.util.Random;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RandomLoadBalance implements LoadBalance {
    private static final Logger log = Logger.getLogger(RandomLoadBalance.class);

    public ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers) {
        if(invokers != null && invokers.size() > 0){
            if(invokers.size() == 1){
                return invokers.get(0);
            }

            Random random = new Random(invokers.size());
            return invokers.get(random.nextInt());
        }

        return null;
    }
}
