package org.kin.kinrpc.serializer.impl;

import com.google.gson.Gson;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class GsonSerializer implements Serializer {
    private Gson gson = new Gson();

    @Override
    public byte[] serialize(Object target) throws IOException {
        return gson.toJson(target).getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException, ClassNotFoundException {
        //不支持不确定类型的反序列化
        return gson.fromJson(new String(bytes), targetClass);
    }

    @Override
    public int type() {
        return SerializerType.GSON.getCode();
    }
}
