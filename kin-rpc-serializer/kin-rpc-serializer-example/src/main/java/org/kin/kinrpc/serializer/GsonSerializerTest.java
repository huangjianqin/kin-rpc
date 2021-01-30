package org.kin.kinrpc.serializer;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class GsonSerializerTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializerType.GSON)
                .run();
    }
}
