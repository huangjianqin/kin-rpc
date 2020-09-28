package org.kin.kinrpc.transport.serializer;

import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.*;

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
     * 根据Serializer class 返回type code
     */
    public static int getSerializerType(Class<? extends Serializer> serializerClass) {
        return SERIALIZER_CACHE.entrySet().stream()
                .filter(e -> e.getValue().getClass().equals(serializerClass))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    /**
     * 根据Serializer class 返回type code, 如果没有加载到, 则主动重新加载
     */
    public static synchronized int getOrLoadSerializer(Class<? extends Serializer> serializerClass) {
        int serializerType = getSerializerType(serializerClass);
        if (serializerType < 0) {
            //主动加载该serializer
            serializerType = loadNew(serializerClass);
        }

        return serializerType;
    }

    /**
     * 加载Serializer
     */
    private static Serializer load0(Map<Integer, Serializer> serializerCache, Class<? extends Serializer> serializerClass) {
        try {
            Serializer serializer = serializerClass.newInstance();
            return load1(serializerCache, serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载Serializer
     */
    private static Serializer load1(Map<Integer, Serializer> serializerCache, Serializer serializer) {
        int type = serializer.type();
        if (serializerCache.containsKey(type)) {
            throw new SerializerTypeConflictException(type, serializer.getClass(), serializerCache.get(type).getClass());
        }
        serializerCache.put(type, serializer);
        return serializer;
    }

    /**
     * 加载已知所有Serializer
     */
    private static void load() {
        Map<Integer, Serializer> serializerCache = new HashMap<>();

        //加载内部提供的Serializer
        Set<Class<? extends Serializer>> classes = ClassUtils.getSubClass(Serializer.class.getPackage().getName(), Serializer.class, true);
        if (classes.size() > 0) {
            for (Class<? extends Serializer> claxx : classes) {
                load0(serializerCache, claxx);
            }
        }

        //通过spi机制加载自定义的Serializer
        Iterator<Serializer> customSerializers = RpcServiceLoader.LOADER.iterator(Serializer.class);
        while (customSerializers.hasNext()) {
            load1(serializerCache, customSerializers.next());
        }

        SERIALIZER_CACHE = Collections.unmodifiableMap(serializerCache);
    }

    /**
     * 加载新的Serializer
     */
    private static int loadNew(Class<? extends Serializer> serializerClass) {
        Map<Integer, Serializer> serializerCache = new HashMap<>(SERIALIZER_CACHE);
        try {
            Serializer serializer = load0(serializerCache, serializerClass);

            SERIALIZER_CACHE = Collections.unmodifiableMap(serializerCache);

            return serializer.type();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
