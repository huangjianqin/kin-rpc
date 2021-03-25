package org.kin.kinrpc.serialization;

import com.google.protobuf.Any;
import org.kin.kinrpc.serialization.protobuf.ProtobufSerialization;
import org.kin.kinrpc.serialization.protobuf.Protobufs;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class ProtoBufSerializationTest {
    public static void main(String[] args) throws IOException {
        Protobufs.register(ProtoBufMessageOuterClass.ProtoBufMessage.getDefaultInstance());

        ProtobufSerialization serialization = new ProtobufSerialization();
        ProtoBufMessageOuterClass.ProtoBufMessage copy = ProtoBufMessageOuterClass.ProtoBufMessage.newBuilder().setA(2).setB("empty").build();
        ProtoBufMessageOuterClass.ProtoBufMessage origin = ProtoBufMessageOuterClass.ProtoBufMessage.newBuilder().setA(1).setB("aa").setData(Any.pack(copy)).build();
        byte[] bytes = serialization.serialize(origin);
        System.out.println(origin);
        ProtoBufMessageOuterClass.ProtoBufMessage deserialize = serialization.deserialize(bytes, ProtoBufMessageOuterClass.ProtoBufMessage.class);
        System.out.println(deserialize);
    }
}
