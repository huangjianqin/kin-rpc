package org.kin.kinrpc.serializer;

import com.google.protobuf.Any;
import org.kin.kinrpc.serializer.protobuf.ProtobufSerializer;
import org.kin.kinrpc.serializer.protobuf.Protobufs;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class ProtoBufSerializerTest {
    public static void main(String[] args) throws IOException {
        Protobufs.register(ProtoBufMessageOuterClass.ProtoBufMessage.getDefaultInstance());

        ProtobufSerializer serializer = new ProtobufSerializer();
        ProtoBufMessageOuterClass.ProtoBufMessage copy = ProtoBufMessageOuterClass.ProtoBufMessage.newBuilder().setA(2).setB("empty").build();
        ProtoBufMessageOuterClass.ProtoBufMessage origin = ProtoBufMessageOuterClass.ProtoBufMessage.newBuilder().setA(1).setB("aa").setData(Any.pack(copy)).build();
        byte[] bytes = serializer.serialize(origin);
        System.out.println(origin);
        ProtoBufMessageOuterClass.ProtoBufMessage deserialize = serializer.deserialize(bytes, ProtoBufMessageOuterClass.ProtoBufMessage.class);
        System.out.println(deserialize);
    }
}
