package org.kin.kinrpc.rpc.protocol.serializer;

import java.io.IOException;

/**
 * Created by 健勤 on 2017/2/10.
 */
public interface Serializer {
    byte[] serialize(Object target) throws IOException;

    Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException;
}
