package org.kin.kinrpc.transport.kinrpc.serializer.impl;

import org.kin.framework.utils.JSON;
import org.kin.kinrpc.transport.kinrpc.serializer.Serializer;
import org.kin.kinrpc.transport.kinrpc.serializer.SerializerType;

import java.io.IOException;

/**
 * todo rpc请求和rpc响应中包含了object 会存在序列化和反序列化结果不一致问题
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
@Deprecated
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
