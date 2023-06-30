package org.kin.kinrpc.transport;

import java.util.concurrent.atomic.AtomicLong;

/**
 * request id generator
 *
 * @author huangjianqin
 * @date 2023/6/1
 */
public final class RequestIdGenerator {
    /** request id generator */
    private static final AtomicLong REQUEST_ID = new AtomicLong();

    private RequestIdGenerator() {
    }

    /**
     * 返回唯一的request id
     */
    public static long next() {
        //递增到负数也是接受的, 唯一即可
        return REQUEST_ID.incrementAndGet();
    }
}
