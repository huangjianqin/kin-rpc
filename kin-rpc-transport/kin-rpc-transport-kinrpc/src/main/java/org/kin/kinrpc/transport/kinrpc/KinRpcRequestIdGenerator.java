package org.kin.kinrpc.transport.kinrpc;

import java.util.UUID;

/**
 * Created by 健勤 on 2017/2/15.
 */
public final class KinRpcRequestIdGenerator {
    private KinRpcRequestIdGenerator() {
    }

    /**
     * 返回唯一的request id
     */
    public static long next() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }
}
