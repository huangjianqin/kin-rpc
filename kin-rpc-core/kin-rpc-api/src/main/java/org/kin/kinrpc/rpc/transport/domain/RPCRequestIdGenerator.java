package org.kin.kinrpc.rpc.transport.domain;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RPCRequestIdGenerator {
    private static AtomicInteger num = new AtomicInteger(0);

    private RPCRequestIdGenerator() {
    }

    public static int next() {
        return num.getAndAdd(1);
    }
}
