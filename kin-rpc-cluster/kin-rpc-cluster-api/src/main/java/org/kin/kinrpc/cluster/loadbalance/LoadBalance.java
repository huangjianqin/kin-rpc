package org.kin.kinrpc.cluster.loadbalance;


import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface LoadBalance {
    ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers);
}
