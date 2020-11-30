package org.kin.kinrpc.serializer.protostuff;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerializer implements Serializer {
    /** 避免每次序列化都重新申请Buffer空间 */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    //----------------------------------------------------------------------------------------------------------------
    @Override
    public byte[] serialize(Object target) {
        Class clazz = target.getClass();
        Schema schema = Protostuffs.getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(target, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }

        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        Schema<T> schema = (Schema<T>) Protostuffs.getSchema(targetClass);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    @Override
    public int type() {
        return SerializerType.PROTOSTUFF.getCode();
    }
}
