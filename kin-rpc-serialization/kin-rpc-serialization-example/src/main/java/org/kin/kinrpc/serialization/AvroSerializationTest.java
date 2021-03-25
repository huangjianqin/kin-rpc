package org.kin.kinrpc.serialization;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class AvroSerializationTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializationType.AVRO)
                .run();
    }
}
