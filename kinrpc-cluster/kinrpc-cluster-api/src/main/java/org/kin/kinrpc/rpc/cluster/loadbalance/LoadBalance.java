package org.kin.kinrpc.rpc.cluster.loadbalance;


import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface LoadBalance {
    AbstractReferenceInvoker loadBalance(List<AbstractReferenceInvoker> invokers);
}
