package org.kin.kinrpc.serialization.protostuff;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerialization implements Serialization {
    /** 避免每次序列化都重新申请Buffer空间 */
    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL = new ThreadLocal<>();
    //----------------------------------------------------------------------------------------------------------------
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public byte[] serialize(Object target) {
        Class clazz = target.getClass();
        Schema schema = Protostuffs.getSchema(clazz);

        if (Objects.isNull(schema)) {
            throw new IllegalStateException("can not find protostuff schema for class ".concat(clazz.getName()));
        }

        LinkedBuffer linkedBuffer = BUFFER_THREAD_LOCAL.get();
        if (Objects.isNull(linkedBuffer)) {
            linkedBuffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
            BUFFER_THREAD_LOCAL.set(linkedBuffer);
        }

        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(target, schema, linkedBuffer);
        } finally {
            linkedBuffer.clear();
        }

        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        Schema<T> schema = (Schema<T>) Protostuffs.getSchema(targetClass);

        if (Objects.isNull(schema)) {
            throw new IllegalStateException("can not find protostuff schema for class ".concat(targetClass.getName()));
        }

        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    @Override
    public int type() {
        return SerializationType.PROTOSTUFF.getCode();
    }
}
