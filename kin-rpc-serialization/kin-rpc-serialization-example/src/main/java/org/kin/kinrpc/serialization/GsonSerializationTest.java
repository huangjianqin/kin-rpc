package org.kin.kinrpc.serialization;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class GsonSerializationTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializationType.GSON)
                .run();
    }
}
