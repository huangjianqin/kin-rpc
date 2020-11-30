package org.kin.kinrpc.serializer;

/**
 * Created by 健勤 on 2017/2/25.
 */
public class KryoSerializeTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializerType.KRYO)
                .run();
    }
}
