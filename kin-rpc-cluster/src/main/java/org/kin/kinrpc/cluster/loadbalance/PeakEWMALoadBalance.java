package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.PeakEWMA;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcCallProfiler;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.PeakEWMAConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于Peak EWMA算法
 * 如果多个invoker有同样的cost, 随机选择即可, 不用考虑权重
 *
 * @author huangjianqin
 * @date 2023/8/19
 */
public class PeakEWMALoadBalance extends AbstractLoadBalance {
    /** 惩罚时间 */
    private static final double PENALTY = Long.MAX_VALUE >> 16;
    /** 模拟0响应时间 */
    private static final double ZERO_RESPONSE = 1E-6;

    /** key -> 服务方法唯一id, value -> {key -> invoker id, value -> {@link Metric}实例} */
    private final ConcurrentMap<Integer, ConcurrentHashMap<Integer, Metric>> metricMap = new ConcurrentHashMap<>(64);

    @Override
    public ReferenceInvoker<?> loadBalance(Invocation invocation, List<ReferenceInvoker<?>> invokers) {
        //default 3s
        long lifeTime = PeakEWMAConstants.DEFAULT_LIFE_TIME;
        ReferenceConfig<?> referenceConfig = invocation.attachment(InvocationConstants.REFERENCE_CONFIG_KEY);
        if (Objects.nonNull(referenceConfig)) {
            lifeTime = referenceConfig.intAttachment(PeakEWMAConstants.LIFE_TIME_KEY, PeakEWMAConstants.DEFAULT_LIFE_TIME);
        }

        //预测的最小rpc请求响应时间
        double minResponse = Double.MAX_VALUE;
        List<ReferenceInvoker<?>> candidateInvokers = new ArrayList<>(invokers.size());
        for (ReferenceInvoker<?> invoker : invokers) {
            //计算预测的响应时间
            double estimateResponse = getEstimateResponse(invocation, lifeTime, invoker);
            //保留一位小数, 截断
            estimateResponse = Math.ceil(estimateResponse * 10) / 10;
            if (estimateResponse < minResponse) {
                candidateInvokers.clear();
                candidateInvokers.add(invoker);
                minResponse = estimateResponse;
            } else if (estimateResponse == minResponse) {
                candidateInvokers.add(invoker);
            }
        }

        return candidateInvokers.get(ThreadLocalRandom.current().nextInt(candidateInvokers.size()));
    }

    /**
     * 返回预测的rpc请求响应时间
     *
     * @param invocation rpc信息
     * @param lifeTime   peak EWMA数据的生命周期时间
     * @param invoker    reference invoker
     * @return 预测的rpc请求响应时间
     */
    private double getEstimateResponse(Invocation invocation,
                                       long lifeTime,
                                       ReferenceInvoker<?> invoker) {
        int invokerId = invoker.hashCode();
        ConcurrentHashMap<Integer, Metric> handlerMap = metricMap.computeIfAbsent(invokerId, k -> new ConcurrentHashMap<>());

        int handlerId = invocation.handlerId();
        RpcCallProfiler rpcCallProfiler = RpcCallProfiler.get(invokerId, handlerId);

        Metric metric = handlerMap.computeIfAbsent(handlerId, k -> new Metric(rpcCallProfiler));
        int active;
        double response;
        synchronized (metric) {
            //remoting rpc call往返平均耗时
            double rtt;
            long succeed = rpcCallProfiler.getSucceeded() - metric.getCallOffset();
            if (succeed != 0) {
                rtt = (rpcCallProfiler.getSucceededElapsed() * 1.0 - metric.getCallElapsedOffset()) / succeed;
            } else {
                //success后的首次fail
                rtt = (rpcCallProfiler.getTotalElapsed() * 1.0 - metric.getCallElapsedOffset());
            }

            if (!metric.init(lifeTime, rtt)) {
                metric.calculate(rtt);
            }
            metric.updateOffset();

            active = rpcCallProfiler.getActive();
            response = metric.getEstimateResponse();
        }

        //active是活跃请求数, 即invoker当前已发起但未完成的rpc request次数
        //如果没有任何统计数据, 则使用active作为预测的响应时间
        //否则, 使用Peak EWMA预测的response time * active, 作为最终预测的响应时间
        //使用active干扰是因为如果预测的响应时间比较少, 但当前活跃请求比较多, 那么意味着可能请求延迟会比较大, 使用active干扰, 让其优先级低一些
        return (response < ZERO_RESPONSE && active != 0) ? PENALTY + active : response * (active + 1);
    }

    //---------------------------------------------------------------------------------------------------------
    private static class Metric {
        /** invoker rpc call统计信息 */
        private final RpcCallProfiler rpcCallProfiler;
        /** Peak EWMA algorithm */
        private PeakEWMA peakEWMA;
        /** 上次计算时, invoker已发起rpc请求总次数 */
        private long callOffset;
        /** 上次计算时, invoker已发起rpc请求总耗时 */
        /**  */
        private long callElapsedOffset;

        public Metric(RpcCallProfiler rpcCallProfiler) {
            this.rpcCallProfiler = rpcCallProfiler;
        }

        /**
         * 初始化
         */
        public boolean init(long lifeTime, double rtt) {
            if (Objects.nonNull(peakEWMA)) {
                return false;
            }

            peakEWMA = new PeakEWMA(lifeTime, rtt);
            return true;
        }

        /**
         * 计算预测的rpc请求响应时间
         */
        public void calculate(double rtt) {
            peakEWMA.observe(rtt);
        }

        /**
         * 更新{@link #callOffset}和{@link #callElapsedOffset}字段值
         */
        public void updateOffset() {
            callOffset = rpcCallProfiler.getTotal();
            callElapsedOffset = rpcCallProfiler.getTotalElapsed();
        }

        /**
         * 返回预测的rpc请求响应时间
         *
         * @return 预测的rpc请求响应时间
         */
        public double getEstimateResponse() {
            return peakEWMA.getEwma();
        }

        //getter
        public long getCallOffset() {
            return callOffset;
        }

        public long getCallElapsedOffset() {
            return callElapsedOffset;
        }
    }
}

