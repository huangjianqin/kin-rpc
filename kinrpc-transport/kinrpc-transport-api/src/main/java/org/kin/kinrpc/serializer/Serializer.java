package org.kin.kinrpc.serializer;

import java.io.IOException;

/**
 * Created by 健勤 on 2017/2/10.
 */
public interface Serializer {
    byte[] serialize(Object target) throws IOException;

    <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException;
}
