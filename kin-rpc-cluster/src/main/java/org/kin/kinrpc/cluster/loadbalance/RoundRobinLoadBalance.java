package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加权顺滑的RoundRobin的负载均衡实现(最大公约数)
 * Created by 健勤 on 2017/2/15.
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {
    /** 回收长期没有selected到的invoker时间(毫秒) */
    private static final int RECYCLE_PERIOD = 60000;
    /** key -> 服务方法唯一id, value -> {key -> invoker id, value -> {@link WeightedRoundRobin}} */
    private final ConcurrentMap<Integer, ConcurrentHashMap<Integer, WeightedRoundRobin>> weightedRoundRobinMap = new ConcurrentHashMap<>();

    @Override
    public ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers) {
        int key = invocation.handlerId();
        ConcurrentHashMap<Integer, WeightedRoundRobin> map = weightedRoundRobinMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        //总权重
        int totalWeight = 0;
        //当前累积权重
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        //最终选择到的invoker
        ReferenceInvoker<?> selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;
        for (ReferenceInvoker<?> invoker : invokers) {
            //invoker id
            int invokerIdentity = invoker.hashCode();
            //invoker权重
            int weight = weight(invoker);
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(invokerIdentity, k -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            if (weight != weightedRoundRobin.weight) {
                //权重发生变化
                weightedRoundRobin.setWeight(weight);
            }
            //增加累积权重
            long cur = weightedRoundRobin.incrCurrent();
            weightedRoundRobin.lastUpdate = now;
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedInvoker = invoker;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;
        }

        if (invokers.size() != map.size()) {
            //invoker list发生变化
            //长时间(1min)没有更新invoker累积权重, 则需要移除, 相当于移除无效invoker, 但是给够时间用于容错(网络断断续续), 防止频繁增删invoker WeightedRoundRobin
            map.entrySet().removeIf(item -> now - item.getValue().lastUpdate > RECYCLE_PERIOD);
        }

        if (selectedInvoker != null) {
            //减少权重累计值
            selectedWRR.descCurrent(totalWeight);
            return selectedInvoker;
        }
        //兜底, 理论上不会到这里
        return invokers.get(0);
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------------

    /** invoker权重计算缓存数据 */
    private static class WeightedRoundRobin {
        /** invoker权重 */
        private volatile int weight;
        /** 当前权重积累值 */
        private final AtomicLong current = new AtomicLong(0);
        /** 上次增加累积权重的时间 */
        private volatile long lastUpdate;

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }

        public long incrCurrent() {
            return current.addAndGet(weight);
        }

        public void descCurrent(int total) {
            current.addAndGet(-1 * total);
        }
    }

}
