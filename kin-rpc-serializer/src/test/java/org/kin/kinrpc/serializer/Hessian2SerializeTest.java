package org.kin.kinrpc.serializer;

import org.kin.kinrpc.serializer.impl.Hessian2Serializer;

import java.io.IOException;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class Hessian2SerializeTest {
    public static void main(String[] args) throws IOException {
        Hessian2Serializer serializer = new Hessian2Serializer();
        byte[] bytes = serializer.serialize(new Type(1, "aa", new Type(0, "empty", null)));

        Type deserializeType = serializer.deserialize(bytes, Type.class);
        System.out.println(deserializeType);

    }
}


