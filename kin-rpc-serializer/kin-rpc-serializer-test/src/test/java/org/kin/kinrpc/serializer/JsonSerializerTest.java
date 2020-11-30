package org.kin.kinrpc.serializer;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class JsonSerializerTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializerType.JSON)
                .afterSerialize(bytes -> System.out.println(new String(bytes)))
                .run();
    }
}
