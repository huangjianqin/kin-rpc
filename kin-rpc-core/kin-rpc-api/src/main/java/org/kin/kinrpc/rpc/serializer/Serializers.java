package org.kin.kinrpc.rpc.serializer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Serializers {
    private static final Logger log = LoggerFactory.getLogger(Serializers.class);
    private static final Cache<String, Serializer> SERIALIZER_CACHE = CacheBuilder.newBuilder().build();

    private Serializers() {
    }

    public static Serializer getSerializer(String type) {
        //从整个classpath寻找Serializer子类
        type = type.toLowerCase();
        try {
            String serializerName = (type + Serializer.class.getSimpleName()).toLowerCase();
            Serializer serializer = SERIALIZER_CACHE.get(type, () -> {
                Set<Class<? extends Serializer>> classes = ClassUtils.getSubClass("", Serializer.class, true);
                if (classes.size() > 0) {
                    for (Class<? extends Serializer> claxx : classes) {
                        String className = claxx.getSimpleName().toLowerCase();
                        if (className.equals(serializerName)) {
                            return claxx.newInstance();
                        }
                    }
                }

                return null;
            });

            return serializer;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        throw new IllegalStateException("init serializer error >>>" + type);
    }
}
