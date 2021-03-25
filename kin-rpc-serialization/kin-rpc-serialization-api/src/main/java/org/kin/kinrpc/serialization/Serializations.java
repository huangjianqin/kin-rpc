package org.kin.kinrpc.serialization;

import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Serializations {
    /** serialization缓存, key -> serialization type, value -> Serialization instance */
    private static volatile Map<Integer, Serialization> SERIALIZATION_CACHE = Collections.emptyMap();

    static {
        load();
    }

    private Serializations() {
    }

    /**
     * @param code 序列化类型
     */
    public static Serialization getSerialization(int code) {
        return SERIALIZATION_CACHE.get(code);
    }

    /**
     * @param name 序列化名称
     */
    public static Serialization getSerialization(String name) {
        Serialization target = SERIALIZATION_CACHE.values().stream()
                .filter(serialization -> serialization.getClass().getSimpleName().toLowerCase().startsWith(name))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(target)) {
            throw new IllegalArgumentException(String.format("unknown Serialization or unable to load Serialization '%s'", name));
        }

        return target;
    }

    /**
     * 根据Serialization class 返回type code
     */
    public static int getSerializationType(Class<? extends Serialization> serializationClass) {
        int serializationType = SERIALIZATION_CACHE.entrySet().stream()
                .filter(e -> e.getValue().getClass().equals(serializationClass))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(0);
        if (serializationType <= 0) {
            throw new IllegalArgumentException(String.format("unknown Serialization or unable to load Serialization '%s'", serializationClass));
        }
        return serializationType;
    }

    /**
     * 加载已注册Serialization
     */
    private static void load() {
        Map<Integer, Serialization> serializationCache = new HashMap<>();

        //通过spi机制加载自定义的Serialization
        for (Serialization serialization : RpcServiceLoader.LOADER.getExtensions(Serialization.class)) {
            int type = serialization.type();
            if (serializationCache.containsKey(type)) {
                throw new SerializationTypeConflictException(type, serialization.getClass(), serializationCache.get(type).getClass());
            }
            serializationCache.put(type, serialization);
        }

        SERIALIZATION_CACHE = Collections.unmodifiableMap(serializationCache);
    }
}
