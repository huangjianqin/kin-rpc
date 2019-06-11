package org.kin.kinrpc.transport.rpc;

import io.netty.util.AttributeKey;
import org.kin.kinrpc.transport.Connection;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public class RPCConstants {
    public static final AttributeKey<Connection> CONNECTION_KEY = AttributeKey.valueOf("connection");
}
