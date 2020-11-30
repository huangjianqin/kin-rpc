package org.kin.kinrpc.serializer.impl;

import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;
import org.kin.kinrpc.serializer.utils.Protobufs;

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
        return SerializerType.PROTOSTUFF.getCode();
    }


}
