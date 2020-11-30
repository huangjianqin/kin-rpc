package org.kin.kinrpc.serializer.protobuf;

import org.kin.kinrpc.serializer.Serializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class ProtobufSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return Protobufs.serialize(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        return Protobufs.deserialize(bytes, targetClass);
    }

    @Override
    public int type() {
        //未用
        return -2;
    }


}
