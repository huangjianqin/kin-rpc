package org.kin.kinrpc.serializer;

import org.kin.kinrpc.serializer.impl.ProtostuffSerializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerializeTest {
    public static void main(String[] args) throws IOException {
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        byte[] bytes = serializer.serialize(new Type(1, "aa", new Type(0, "empty", null)));

        Type deserializeType = serializer.deserialize(bytes, Type.class);
        System.out.println(deserializeType);

    }
}
