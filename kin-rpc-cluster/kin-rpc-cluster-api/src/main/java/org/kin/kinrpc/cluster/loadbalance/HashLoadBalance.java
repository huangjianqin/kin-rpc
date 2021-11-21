package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.HashUtils;
import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.TreeMap;

/**
 * 基于hash的负载均衡实现
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashLoadBalance implements LoadBalance {
    private static final int LIMIT = 9;

    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        TreeMap<Integer, AsyncInvoker> map = new TreeMap<>();
        for (AsyncInvoker AsyncInvoker : invokers) {
            map.put(HashUtils.efficientHash(AsyncInvoker, LIMIT), AsyncInvoker);
        }

        return map.firstEntry().getValue();
    }
}
