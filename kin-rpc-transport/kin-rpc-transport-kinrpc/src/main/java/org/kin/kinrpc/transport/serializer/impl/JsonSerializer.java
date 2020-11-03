package org.kin.kinrpc.transport.serializer.impl;

import org.kin.framework.utils.JSON;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.kinrpc.transport.serializer.SerializerType;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return JSON.writeBytes(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        return JSON.read(bytes, targetClass);
    }

    @Override
    public int type() {
        return SerializerType.JSON.getCode();
    }
}
