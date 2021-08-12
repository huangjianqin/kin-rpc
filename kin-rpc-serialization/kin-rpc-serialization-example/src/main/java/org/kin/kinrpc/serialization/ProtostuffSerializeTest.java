package org.kin.kinrpc.serialization;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerializeTest {
    public static void main(String[] args) throws IOException {
        SerializeTestBase
                .builder(SerializationType.PROTOBUF)
                .run();

    }
}
