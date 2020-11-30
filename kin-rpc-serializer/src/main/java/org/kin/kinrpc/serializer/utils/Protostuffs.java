package org.kin.kinrpc.serializer.utils;

import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020/11/30
 */
public class Protostuffs {
    private Protostuffs() {
    }

    /** 缓存Schema */
    private static final Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    /**
     * 注册class, 分析其schema, 并缓存
     * 可用于提前注册好schema, 运行时, 避免分析class, 提高性能
     * !!! 仅仅支持Serializer初始化前注册
     */
    public static void register(Class<?> clazz) {
        getSchema(clazz);
    }

    /**
     * 获取schema
     */
    public static Schema<?> getSchema(Class<?> clazz) {
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
}
