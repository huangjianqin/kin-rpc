package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.framework.utils.HashUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.List;
import java.util.TreeMap;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashLoadBalance implements LoadBalance {
    private static final int LIMIT = 9;

    @Override
    public AbstractReferenceInvoker loadBalance(List<AbstractReferenceInvoker> invokers) {
        TreeMap<Integer, AbstractReferenceInvoker> map = new TreeMap<>();
        for(AbstractReferenceInvoker invoker: invokers){
            map.put(HashUtils.efficientHash(invoker, LIMIT), invoker);
        }

        return map.firstEntry().getValue();
    }
}
