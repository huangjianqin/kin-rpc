package org.kin.kinrpc.serializer.kryo;

import org.kin.framework.collection.ConcurrentHashSet;

import java.util.Set;

/**
 * @author huangjianqin
 * @date 2020/11/30
 */
public class Kryos {
    private Kryos() {
    }

    /** 缓存注册的class */
    private static final ConcurrentHashSet<Class<?>> REGISTERED_CLASS = new ConcurrentHashSet<>();

    /**
     * 注册class, 分析其schema, 并缓存
     * 可用于提前注册好class, 运行时, 避免分析class, 提高性能
     * !!! 仅仅支持Serializer初始化前注册
     */
    public static void register(Class<?> clazz) {
        REGISTERED_CLASS.add(clazz);
    }

    public static Set<Class<?>> getRegisteredClass() {
        return REGISTERED_CLASS;
    }
}
