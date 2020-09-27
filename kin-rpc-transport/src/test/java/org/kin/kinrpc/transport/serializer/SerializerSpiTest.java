package org.kin.kinrpc.transport.serializer;

import org.project.serializer.MySerializer;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializerSpiTest {
    public static void main(String[] args) {
        System.out.println(Serializers.getSerializerType(MySerializer.class));
    }
}
