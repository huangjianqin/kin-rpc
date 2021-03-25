package org.kin.kinrpc.serialization.protobuf;

import org.kin.kinrpc.serialization.Serialization;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class ProtobufSerialization implements Serialization {
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
