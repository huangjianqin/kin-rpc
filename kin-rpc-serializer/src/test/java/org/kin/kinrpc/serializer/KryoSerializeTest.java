package org.kin.kinrpc.serializer;

import org.kin.kinrpc.serializer.impl.KryoSerializer;

/**
 * Created by 健勤 on 2017/2/25.
 */
public class KryoSerializeTest {
    public static void main(String[] args) {
        KryoSerializer serializer = new KryoSerializer();
        byte[] bytes = serializer.serialize(new Type(1, "aa", new Type(0, "empty", null)));

        Type deserializeType = serializer.deserialize(bytes, Type.class);
        System.out.println(deserializeType);
    }
}
