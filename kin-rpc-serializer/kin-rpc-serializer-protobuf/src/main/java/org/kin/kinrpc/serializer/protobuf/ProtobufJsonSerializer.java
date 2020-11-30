package org.kin.kinrpc.serializer.protobuf;

import org.kin.kinrpc.serializer.Serializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class ProtobufJsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return Protobufs.serializeJson(target).getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        return Protobufs.deserializeJson(new String(bytes), targetClass);
    }

    @Override
    public int type() {
        //未用
        return -1;
    }


}