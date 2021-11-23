package org.kin.kinrpc.serialization.protobuf;

import com.google.protobuf.MessageLite;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;

import java.io.IOException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
@Extension(value = "protobuf", code = 5)
public class ProtobufSerialization implements Serialization {
    /** 避免每次序列化都重新申请Buffer空间 */
    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public byte[] serialize(Object target) throws IOException {
        if (Objects.isNull(target)) {
            throw new IllegalStateException("serialize object is null");
        }

        if (target instanceof MessageLite) {
            //以protobuf序列化
            return protobufSerialize(target);
        } else {
            //以protostuff序列化
            return protostuffSerialize(target);
        }
    }

    /**
     * 以protobuf序列化
     */
    private byte[] protobufSerialize(Object target) throws IOException {
        return Protobufs.serialize(target);
    }

    /**
     * 以protostuff序列化
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private byte[] protostuffSerialize(Object target) {
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
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        if (MessageLite.class.isAssignableFrom(targetClass)) {
            //以protobuf反序列化
            return protobufDeserialize(bytes, targetClass);
        } else {
            //以protostuff反序列化
            return protostuffDeserialize(bytes, targetClass);
        }
    }

    /**
     * 以protobuf反序列化
     */
    private <T> T protobufDeserialize(byte[] bytes, Class<T> targetClass) {
        return Protobufs.deserialize(bytes, targetClass);
    }

    /**
     * 以protostuff反序列化
     */
    @SuppressWarnings("unchecked")
    private <T> T protostuffDeserialize(byte[] bytes, Class<T> targetClass) {
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
        return SerializationType.PROTOBUF.getCode();
    }
}
