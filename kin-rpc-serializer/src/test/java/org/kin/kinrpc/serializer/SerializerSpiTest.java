package org.kin.kinrpc.serializer;

import org.custom.serializer.MySerializer;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializerSpiTest {
    public static void main(String[] args) {
        System.out.println(Serializers.getSerializerType(MySerializer.class));
    }
}
