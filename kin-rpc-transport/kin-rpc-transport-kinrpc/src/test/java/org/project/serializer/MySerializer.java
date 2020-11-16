package org.project.serializer;

import org.kin.kinrpc.transport.kinrpc.serializer.Serializer;

import java.io.IOException;

/**
 * 模拟外部使用自定义Serializer, 因此包名也做模拟
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class MySerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException, ClassNotFoundException {
        return null;
    }

    @Override
    public int type() {
        return 10;
    }
}
