package org.kin.kinrpc.serializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerializeTest {
    public static void main(String[] args) throws IOException {
        SerializeTestBase
                .builder(SerializerType.PROTOSTUFF)
                .run();

    }
}
