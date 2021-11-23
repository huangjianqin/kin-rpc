package org.kin.kinrpc.serialization.protobuf;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.serialization.Serialization;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
@Extension(value = "protobufJson", code = -1)
public class ProtobufJsonSerialization implements Serialization {
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