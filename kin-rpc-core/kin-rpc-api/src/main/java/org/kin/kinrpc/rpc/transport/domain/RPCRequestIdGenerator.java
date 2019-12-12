package org.kin.kinrpc.rpc.transport.domain;

import java.util.UUID;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RPCRequestIdGenerator {
    private RPCRequestIdGenerator() {
    }

    public static String next() {
        return UUID.randomUUID().toString();
    }
}
