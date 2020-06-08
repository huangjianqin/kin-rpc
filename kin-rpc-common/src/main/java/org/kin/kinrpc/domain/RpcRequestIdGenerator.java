package org.kin.kinrpc.domain;

import java.util.UUID;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RpcRequestIdGenerator {
    private RpcRequestIdGenerator() {
    }

    public static long next() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }
}
