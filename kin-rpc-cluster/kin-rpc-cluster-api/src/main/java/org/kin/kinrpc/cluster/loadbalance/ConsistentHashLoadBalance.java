package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2021/11/21
 */
@SuppressWarnings("rawtypes")
public class ConsistentHashLoadBalance implements LoadBalance {
    private final ConcurrentHashMap<String, ConsistentHash> consistentHashMap = new ConcurrentHashMap<>();

    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        String key = serviceKey + "#" + method;
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
        private int hashCode;

        public ConsistentHash(List<AsyncInvoker> invokers, int hashCode) {
            super(HASH_NODE_NUM);

            for (AsyncInvoker invoker : invokers) {
                add(invoker);
            }
            this.hashCode = hashCode;
        }

        @Override
        public void add(AsyncInvoker obj, int weight) {
            if (hashCode == 0) {
                //初始化
                super.add(obj, weight);
                return;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(AsyncInvoker obj, int weight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(AsyncInvoker obj) {
            if (hashCode == 0) {
                //初始化
                super.add(obj);
                return;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(AsyncInvoker obj) {
            throw new UnsupportedOperationException();
        }
    }
}
