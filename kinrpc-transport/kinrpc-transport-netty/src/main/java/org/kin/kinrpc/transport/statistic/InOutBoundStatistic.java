package org.kin.kinrpc.transport.statistic;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public class InOutBoundStatistic {
    /**
     * serviceName+method || protocolId
     */
    private String uuid;
    private AtomicLong totalSize;
    private AtomicLong count;

    public InOutBoundStatistic(String uuid) {
        this.uuid = uuid;
    }

    public void incr(long size) {
        totalSize.addAndGet(size);
        count.incrementAndGet();
    }

    @Override
    public String toString() {
        return "uuid: " + uuid + ">>>" + totalSize + "-" + count;
    }
}
