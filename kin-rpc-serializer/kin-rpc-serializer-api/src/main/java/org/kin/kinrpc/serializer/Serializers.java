package org.kin.kinrpc.serializer;

import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Serializers {
    /** serializer缓存, key -> serializer type, value -> Serializer instance */
    private static volatile Map<Integer, Serializer> SERIALIZER_CACHE = Collections.emptyMap();

    static {
        load();
    }

    private Serializers() {
    }

    /**
     * @param code 序列化类型
     */
    public static Serializer getSerializer(int code) {
        return SERIALIZER_CACHE.get(code);
    }

    /**
     * @param name 序列化名称
     */
    public static Serializer getSerializer(String name) {
        Serializer target = SERIALIZER_CACHE.values().stream()
                .filter(serializer -> serializer.getClass().getSimpleName().toLowerCase().startsWith(name))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(target)) {
            throw new IllegalArgumentException(String.format("unknown Serializer or unable to load Serializer '%s'", name));
        }

        return target;
    }

    /**
     * 根据Serializer class 返回type code
     */
    public static int getSerializerType(Class<? extends Serializer> serializerClass) {
        int serializerType = SERIALIZER_CACHE.entrySet().stream()
                .filter(e -> e.getValue().getClass().equals(serializerClass))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(0);
        if (serializerType <= 0) {
            throw new IllegalArgumentException(String.format("unknown Serializer or unable to load Serializer '%s'", serializerClass));
        }
        return serializerType;
    }

    /**
     * 加载已注册Serializer
     */
    private static void load() {
        Map<Integer, Serializer> serializerCache = new HashMap<>();

        //通过spi机制加载自定义的Serializer
        for (Serializer serializer : RpcServiceLoader.LOADER.getExtensions(Serializer.class)) {
            int type = serializer.type();
            if (serializerCache.containsKey(type)) {
                throw new SerializerTypeConflictException(type, serializer.getClass(), serializerCache.get(type).getClass());
            }
            serializerCache.put(type, serializer);
        }

        SERIALIZER_CACHE = Collections.unmodifiableMap(serializerCache);
    }
}
