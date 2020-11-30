package org.kin.kinrpc.serializer;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class AvroSerializerTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializerType.AVRO)
                .run();
    }
}
