package org.kin.kinrpc.serialization;

import org.kin.framework.utils.AbstractExtensionCache;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public final class Serializations extends AbstractExtensionCache<Integer, Serialization> {
    //单例
    public static final Serializations INSTANCE = new Serializations();

    public Serializations() {
        super(RpcServiceLoader.LOADER);
    }

    @Override
    protected Integer[] keys(Serialization extension) {
        return new Integer[]{extension.type()};
    }

    /**
     * @param name 序列化名称
     */
    public Serialization getSerialization(String name) {
        Serialization target = cache.values().stream()
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
    public int getSerializationType(Class<? extends Serialization> serializationClass) {
        int serializationType = cache.entrySet().stream()
                .filter(e -> e.getValue().getClass().equals(serializationClass))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(0);
        if (serializationType <= 0) {
            throw new IllegalArgumentException(String.format("unknown Serialization or unable to load Serialization '%s'", serializationClass));
        }
        return serializationType;
    }
}
