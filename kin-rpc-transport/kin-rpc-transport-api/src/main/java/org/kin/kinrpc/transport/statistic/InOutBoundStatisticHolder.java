package org.kin.kinrpc.transport.statistic;

import org.kin.framework.concurrent.Partitioner;
import org.kin.framework.concurrent.impl.EfficientHashPartitioner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public class InOutBoundStatisticHolder {
    private AtomicLong ref = new AtomicLong(0);
    private Map<String, InOutBoundStatistic> statisticMap = new HashMap<>();

    private static final byte LOCK_NUM = 5;
    private final Object[] locks = new Object[LOCK_NUM];
    private final Partitioner<String> partitioner = new EfficientHashPartitioner<>();

    InOutBoundStatisticHolder() {
        for(int i = 0; i < locks.length; i++){
            locks[i] = new Object();
        }
    }

    InOutBoundStatistic getstatistic(String uuid) {
        if (!statisticMap.containsKey(uuid)) {
            Object lock = locks[partitioner.toPartition(uuid, LOCK_NUM)];
            synchronized (lock) {
                if (!statisticMap.containsKey(uuid)) {
                    statisticMap.put(uuid, new InOutBoundStatistic(uuid));
                }
            }
        }

        return statisticMap.get(uuid);
    }

    String logContent() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        for (InOutBoundStatistic statistic : statisticMap.values()) {
            sb.append(statistic.toString() + System.lineSeparator());
        }
        return sb.toString();
    }

    void reference() {
        ref.incrementAndGet();
    }

    void release() {
        ref.decrementAndGet();
    }

    long getRef() {
        return ref.get();
    }
}
