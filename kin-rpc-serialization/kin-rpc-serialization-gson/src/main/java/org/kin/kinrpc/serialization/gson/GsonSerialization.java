package org.kin.kinrpc.serialization.gson;

import com.google.gson.Gson;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
@Extension(value = "gson", code = 6)
public class GsonSerialization implements Serialization {
    private final Gson gson = new Gson();

    @Override
    public byte[] serialize(Object target) {
        return gson.toJson(target).getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        //不支持不确定类型的反序列化
        return gson.fromJson(new String(bytes), targetClass);
    }

    @Override
    public int type() {
        return SerializationType.GSON.getCode();
    }
}
