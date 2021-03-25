package org.kin.kinrpc.serialization;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class JsonSerializationTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializationType.JSON)
                .afterSerialize(bytes -> System.out.println(new String(bytes)))
                .run();
    }
}
