package org.kin.kinrpc.transport.serializer;

import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Serializers {
    private static final Logger log = LoggerFactory.getLogger(Serializers.class);
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
     * 加载已知所有Serializer
     */
    public static void load() {
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
}
