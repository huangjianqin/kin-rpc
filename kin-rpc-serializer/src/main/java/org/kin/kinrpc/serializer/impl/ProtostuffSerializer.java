package org.kin.kinrpc.serializer.impl;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class ProtostuffSerializer implements Serializer {
    /** 避免每次序列化都重新申请Buffer空间 */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    /** 缓存Schema */
    private static final Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取schema
     */
    private static Schema<?> getSchema(Class<?> clazz) {
        Schema<?> schema = SCHEMA_CACHE.get(clazz);
        if (Objects.isNull(schema)) {
            //RuntimeSchema进行懒创建并缓存
            //该方法是线程安全的
            schema = RuntimeSchema.getSchema(clazz);
            if (Objects.nonNull(schema)) {
                SCHEMA_CACHE.put(clazz, schema);
            }
        }

        return schema;
    }

    //----------------------------------------------------------------------------------------------------------------

    @Override
    public byte[] serialize(Object target) {
        Class clazz = target.getClass();
        Schema schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(target, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }

        return data;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        Schema<T> schema = (Schema<T>) getSchema(targetClass);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    @Override
    public int type() {
        return SerializerType.PROTOSTUFF.getCode();
    }
}
