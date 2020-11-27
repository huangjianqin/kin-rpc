package org.kin.kinrpc.serializer;

import org.kin.kinrpc.serializer.impl.JsonSerializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class JsonSerializerTest {
    public static void main(String[] args) throws IOException {
        JsonSerializer jsonSerializer = new JsonSerializer();
        String jsonStr = new String(jsonSerializer.serialize(new Type(1, "aa", new Type(0, "empty", null))));
        System.out.println(jsonStr);
        Type deserializeType = jsonSerializer.deserialize(jsonStr.getBytes(), Type.class);
        System.out.println(deserializeType);
    }
}
