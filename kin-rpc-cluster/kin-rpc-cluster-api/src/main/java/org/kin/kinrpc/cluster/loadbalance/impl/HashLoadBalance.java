package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.framework.utils.HashUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.TreeMap;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashLoadBalance implements LoadBalance {
    private static final int LIMIT = 9;

    @Override
    public AsyncInvoker loadBalance(List<AsyncInvoker> invokers) {
        TreeMap<Integer, AsyncInvoker> map = new TreeMap<>();
        for (AsyncInvoker AsyncInvoker : invokers) {
            map.put(HashUtils.efficientHash(AsyncInvoker, LIMIT), AsyncInvoker);
        }

        return map.firstEntry().getValue();
    }
}
