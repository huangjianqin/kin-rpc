package org.kin.kinrpc.cluster.loadbalance;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于加权random的负载均衡实现(二分法，不要遍历)
 * Created by 健勤 on 2017/2/15.
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    public ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers) {
        //invoker数量
        int length = invokers.size();
        if (length == 0) {
            return null;
        }
        //是否所有invoker相同权重
        boolean sameWeight = true;
        //每个invoker的权重
        int[] weights = new int[length];
        //第一个invoker的权重
        int firstWeight = weight(invokers.get(0));
        weights[0] = firstWeight;
        //总权重
        int totalWeight = firstWeight;
        for (int i = 1; i < length; i++) {
            int weight = weight(invokers.get(i));
            weights[i] = weight;
            //计算总权重
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            //权重不完全一样 & 至少一个invoker权重>0
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < length; i++) {
                offset -= weights[i];
                if (offset < 0) {
                    return invokers.get(i);
                }
            }
        }
        //纯随机
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }
}
