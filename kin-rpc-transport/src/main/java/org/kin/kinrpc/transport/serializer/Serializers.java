package org.kin.kinrpc.transport.serializer;

import org.kin.framework.utils.ClassUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Serializers {
    /** serializer缓存 */
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
     * 加载已知所有Serializer
     */
    private static void load() {
        //从整个classpath寻找Serializer子类
        Set<Class<? extends Serializer>> classes = ClassUtils.getSubClass(Serializer.class.getPackage().getName(), Serializer.class, true);
        //TODO 考虑增加加载外部自定义的Serializer 通过spi方式
        if (classes.size() > 0) {
            Map<Integer, Serializer> serializerCache = new HashMap<>();
            for (Class<? extends Serializer> claxx : classes) {
                try {
                    Serializer serializer = claxx.newInstance();
                    serializerCache.put(serializer.type(), serializer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            SERIALIZER_CACHE = Collections.unmodifiableMap(serializerCache);
        }
    }

    /**
     * 加载新的Serializer
     */
    private static int loadNew(Class<? extends Serializer> serializerClass) {
        Map<Integer, Serializer> serializerCache = new HashMap<>(SERIALIZER_CACHE);
        try {
            Serializer serializer = serializerClass.newInstance();
            serializerCache.put(serializer.type(), serializer);

            SERIALIZER_CACHE = Collections.unmodifiableMap(serializerCache);

            return serializer.type();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
