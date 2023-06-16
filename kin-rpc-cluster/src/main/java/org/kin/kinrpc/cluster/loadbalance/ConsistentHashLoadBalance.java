package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于一致性hash的负载均衡实现(有状态服务场景)
 *
 * @author huangjianqin
 * @date 2021/11/21
 */
@SuppressWarnings("rawtypes")
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    private final ConcurrentHashMap<Integer, ConsistentHash> consistentHashMap = new ConcurrentHashMap<>();

    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        int key = key(serviceKey, method);
        int hashCode = invokers.hashCode();
        ConsistentHash consistentHash = consistentHashMap.get(key);
        if (Objects.isNull(consistentHash) || consistentHash.hashCode != hashCode) {
            consistentHash = new ConsistentHash(invokers, hashCode);
            consistentHashMap.put(key, consistentHash);
        }

        //以第一个参数作为hash对象
        return consistentHash.get(params[0]);
    }

    /**
     * 不可变hash环
     * 如果发现{@link AsyncInvoker} list发生变化时, 直接替换
     */
    private static class ConsistentHash extends org.kin.framework.utils.ConsistentHash<AsyncInvoker> {
        /** hash环每个节点数量(含虚拟节点) */
        private static final int HASH_NODE_NUM = 128;
        /** 标识该hash环对应的{@link AsyncInvoker} list, 用于判断{@link AsyncInvoker} list是否发生变化 */
        private final int hashCode;

        public ConsistentHash(List<AsyncInvoker> invokers, int hashCode) {
            super(HASH_NODE_NUM);

            for (AsyncInvoker invoker : invokers) {
                add(invoker);
            }
            this.hashCode = hashCode;
        }

        @Override
        protected void applySlot(SortedMap<Long, AsyncInvoker> circle, String s, AsyncInvoker node) {
            if (hashCode == 0) {
                //初始化
                super.applySlot(circle, s, node);
                return;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeSlot(SortedMap<Long, AsyncInvoker> circle, String s) {
            if (hashCode == 0) {
                //初始化
                super.removeSlot(circle, s);
                return;
            }
            throw new UnsupportedOperationException();
        }
    }
}
