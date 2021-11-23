package org.custom.serialization;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.serialization.Serialization;

import java.io.IOException;

/**
 * 模拟外部使用自定义Serialization, 因此包名也做模拟
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
@Extension(value = "my", code = 100)
public class MySerialization implements Serialization {
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
