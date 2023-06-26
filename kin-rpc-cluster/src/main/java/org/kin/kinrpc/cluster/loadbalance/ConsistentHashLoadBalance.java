package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

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
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    /** key -> handler id, value -> 该服务方法对应的一致性hash环 */
    private final ConcurrentHashMap<Integer, ConsistentHash> consistentHashMap = new ConcurrentHashMap<>();

    @Override
    public ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers) {
        int handlerId = invocation.handlerId();
        int hashCode = invokers.hashCode();
        ConsistentHash consistentHash = consistentHashMap.get(handlerId);
        if (Objects.isNull(consistentHash) || consistentHash.hashCode != hashCode) {
            consistentHash = new ConsistentHash(invokers, hashCode);
            consistentHashMap.put(handlerId, consistentHash);
        }

        //以参数作为hash对象
        return consistentHash.get(invocation.params());
    }

    /**
     * 不可变hash环
     * 如果发现invoker list发生变化时, 直接替换
     */
    private static class ConsistentHash extends org.kin.framework.utils.ConsistentHash<ReferenceInvoker<?>> {
        /** hash环每个节点数量(含虚拟节点) */
        private static final int HASH_NODE_NUM = 128;
        /** hash环唯一标识, 即hashCode(list(invoker)), 用于判断invoker list是否发生变化 */
        private final int hashCode;

        public ConsistentHash(List<ReferenceInvoker<?>> invokers, int hashCode) {
            super(HASH_NODE_NUM);

            for (ReferenceInvoker<?> invoker : invokers) {
                add(invoker);
            }
            this.hashCode = hashCode;
        }

        @Override
        protected void applySlot(SortedMap<Long, ReferenceInvoker<?>> circle, String s, ReferenceInvoker<?> node) {
            if (hashCode == 0) {
                //初始化
                super.applySlot(circle, s, node);
                return;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeSlot(SortedMap<Long, ReferenceInvoker<?>> circle, String s) {
            if (hashCode == 0) {
                //初始化
                super.removeSlot(circle, s);
                return;
            }
            throw new UnsupportedOperationException();
        }
    }
}
