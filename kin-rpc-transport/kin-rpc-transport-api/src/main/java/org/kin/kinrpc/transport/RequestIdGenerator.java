package org.kin.kinrpc.transport;

import java.util.UUID;

/**
 * request id generator
 *
 * @author huangjianqin
 * @date 2023/6/1
 */
public final class RequestIdGenerator {
    private RequestIdGenerator() {
    }

    /**
     * 返回唯一的request id
     */
    public static long next() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }
}
