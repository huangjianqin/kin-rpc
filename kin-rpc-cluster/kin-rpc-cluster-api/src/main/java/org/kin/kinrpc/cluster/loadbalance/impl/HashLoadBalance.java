package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.framework.utils.HashUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;
import java.util.TreeMap;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashLoadBalance implements LoadBalance {
    private static final int LIMIT = 9;

    @Override
    public ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers) {
        TreeMap<Integer, ReferenceInvoker> map = new TreeMap<>();
        for (ReferenceInvoker invoker : invokers) {
            map.put(HashUtils.efficientHash(invoker, LIMIT), invoker);
        }

        return map.firstEntry().getValue();
    }
}
