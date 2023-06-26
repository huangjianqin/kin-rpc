package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.HashUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;
import java.util.TreeMap;

/**
 * 基于hash的负载均衡实现
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashLoadBalance implements LoadBalance {
    @Override
    public ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers) {
        TreeMap<Integer, ReferenceInvoker<?>> map = new TreeMap<>();
        for (ReferenceInvoker<?> invoker : invokers) {
            map.put(HashUtils.efficientHash(invoker, invokers.size()), invoker);
        }

        return map.firstEntry().getValue();
    }
}
