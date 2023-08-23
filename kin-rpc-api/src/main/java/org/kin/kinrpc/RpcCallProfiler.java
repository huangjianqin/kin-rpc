package org.kin.kinrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ReferenceInvoker} rpc call信息统计
 * <p>
 * !!!注意, 这里统计的是remoting rpc call相关信息
 *
 * @author huangjianqin
 * @date 2023/8/18
 */
public final class RpcCallProfiler {
    private static final Logger log = LoggerFactory.getLogger(RpcCallProfiler.class);
    /**
     * 服务维度的统计
     * key -> {@link ReferenceInvoker}唯一id, value -> 统计信息
     */
    private static final ConcurrentMap<Integer, RpcCallProfiler> INVOKER_STATISTICS = new ConcurrentHashMap<>(16);
    /**
     * 服务方法维度的统计
     * key -> {@link ReferenceInvoker}唯一id, value -> {key -> 服务方法唯一id, value -> 统计信息}
     */
    private static final ConcurrentMap<Integer, ConcurrentMap<Integer, RpcCallProfiler>> HANDLER_STATISTICS =
            new ConcurrentHashMap<>(64);

    /** 活跃数, 即当前已发起但未结束的rpc call次数 */
    private final AtomicInteger active = new AtomicInteger();
    /** 已完成的rpc call次数 */
    private final AtomicLong total = new AtomicLong();
    /** rpc call失败次数 */
    private final AtomicLong failed = new AtomicLong();
    /** rpc call总耗时 */
    private final AtomicLong totalElapsed = new AtomicLong();
    /** rpc call失败总耗时 */
    private final AtomicLong failedElapsed = new AtomicLong();
    /** rpc call最大耗时 */
    private final AtomicLong maxElapsed = new AtomicLong();
    /** rpc call失败最大耗时 */
    private final AtomicLong failedMaxElapsed = new AtomicLong();
    /** rpc call成功最大耗时 */
    private final AtomicLong succeededMaxElapsed = new AtomicLong();

    /**
     * 返回服务维度的统计
     *
     * @param invokerId reference invoker id
     * @return 统计信息
     */
    public static RpcCallProfiler get(int invokerId) {
        return INVOKER_STATISTICS.computeIfAbsent(invokerId, k -> new RpcCallProfiler());
    }

    /**
     * 返回服务方法维度的统计
     *
     * @param invokerId reference invoker id
     * @param handlerId 服务方法唯一id
     * @return 统计信息
     */
    public static RpcCallProfiler get(int invokerId, int handlerId) {
        ConcurrentMap<Integer, RpcCallProfiler> handlerMap = HANDLER_STATISTICS.computeIfAbsent(invokerId, k -> new ConcurrentHashMap<>());
        return handlerMap.computeIfAbsent(handlerId, k -> new RpcCallProfiler());
    }

    /**
     * 移除服务维度的统计
     *
     * @param invokerId reference invoker id
     * @return 原统计信息
     */
    @Nullable
    public static RpcCallProfiler remove(int invokerId) {
        return INVOKER_STATISTICS.remove(invokerId);
    }

    /**
     * 移除服务方法维度的统计
     *
     * @param invokerId reference invoker id
     * @param handlerId 服务方法唯一id
     * @return 原统计信息
     */
    @Nullable
    public static RpcCallProfiler remove(int invokerId, int handlerId) {
        ConcurrentMap<Integer, RpcCallProfiler> handlerMap = HANDLER_STATISTICS.computeIfAbsent(invokerId, k -> new ConcurrentHashMap<>());
        return handlerMap.remove(handlerId);
    }

    /**
     * 开始统计
     *
     * @param invokerId reference invoker id
     * @param handlerId 服务方法唯一id
     */
    public static void watch(int invokerId, int handlerId) {
        watch(invokerId, handlerId, Integer.MAX_VALUE);
    }

    /**
     * 开始统计
     *
     * @param invokerId reference invoker id
     * @param handlerId 服务方法唯一id
     * @return 是否统计成功
     */
    public static boolean watch(int invokerId, int handlerId, int max) {
        max = (max <= 0) ? Integer.MAX_VALUE : max;
        RpcCallProfiler serviceStatus = get(invokerId);
        RpcCallProfiler handlerStatus = get(invokerId, handlerId);

        //active + 1
        for (int i; ; ) {
            i = handlerStatus.active.get();

            //active到max了
            if (i == Integer.MAX_VALUE || i + 1 > max) {
                return false;
            }

            if (handlerStatus.active.compareAndSet(i, i + 1)) {
                break;
            }
        }

        serviceStatus.active.incrementAndGet();

        return true;
    }

    /**
     * 结束统计
     *
     * @param invokerId reference invoker id
     * @param handlerId 服务方法唯一id
     * @param elapsed   rpc call耗时
     * @param succeeded rpc call是否成功
     */
    public static void end(int invokerId, int handlerId, long elapsed, boolean succeeded) {
        end(get(invokerId), elapsed, succeeded);
        end(get(invokerId, handlerId), elapsed, succeeded);
    }

    /**
     * 结束统计
     *
     * @param status    rpc call统计
     * @param elapsed   rpc call耗时
     * @param succeeded rpc call是否成功
     */
    private static void end(RpcCallProfiler status, long elapsed, boolean succeeded) {
        //active - 1
        status.active.decrementAndGet();
        //total + 1
        status.total.incrementAndGet();
        //total elapsed +
        status.totalElapsed.addAndGet(elapsed);

        //update max elapsed
        if (status.maxElapsed.get() < elapsed) {
            status.maxElapsed.set(elapsed);
        }

        if (succeeded) {
            //update succeeded max elapsed
            if (status.succeededMaxElapsed.get() < elapsed) {
                status.succeededMaxElapsed.set(elapsed);
            }

        } else {
            //failed + 1
            status.failed.incrementAndGet();
            //failed elapsed +
            status.failedElapsed.addAndGet(elapsed);
            //update failed max elapsed
            if (status.failedMaxElapsed.get() < elapsed) {
                status.failedMaxElapsed.set(elapsed);
            }
        }
    }

    /**
     * 日志输出, 一般用于测试或运维
     */
    public static String log() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, RpcCallProfiler> entry : INVOKER_STATISTICS.entrySet()) {
            log.info("invoker statistics, invokerId={}, statistics={}", entry.getKey(), entry.getValue());
            sb.append(String.format("invoker statistics, invokerId=%d, statistics=%s", entry.getKey(), entry.getValue()));
            sb.append(System.lineSeparator());
        }

        for (Map.Entry<Integer, ConcurrentMap<Integer, RpcCallProfiler>> entry1 : HANDLER_STATISTICS.entrySet()) {
            for (Map.Entry<Integer, RpcCallProfiler> entry2 : entry1.getValue().entrySet()) {
                log.info("handler statistics, invokerId={}, handlerId={}, statistics={}", entry1.getKey(), entry2.getKey(), entry2.getValue());
                sb.append(String.format("handler statistics, invokerId=%d, handlerId=%d, statistics=%s", entry1.getKey(), entry2.getKey(), entry2.getValue()));
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    //getter
    public int getActive() {
        return active.get();
    }

    public long getTotal() {
        return total.longValue();
    }

    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    public long getFailed() {
        return failed.get();
    }

    public long getFailedElapsed() {
        return failedElapsed.get();
    }

    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    @Override
    public String toString() {
        return "{" +
                "active=" + active.get() +
                ", total=" + total.get() +
                ", failed=" + failed.get() +
                ", totalElapsed=" + totalElapsed.get() +
                ", failedElapsed=" + failedElapsed.get() +
                ", maxElapsed=" + maxElapsed.get() +
                ", failedMaxElapsed=" + failedMaxElapsed.get() +
                ", succeededMaxElapsed=" + succeededMaxElapsed.get() +
                '}';
    }
}
