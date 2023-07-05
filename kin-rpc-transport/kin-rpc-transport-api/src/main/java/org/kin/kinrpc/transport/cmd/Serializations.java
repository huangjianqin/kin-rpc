package org.kin.kinrpc.transport.cmd;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.serialization.Serialization;

import java.util.Objects;

/**
 * 序列化相关工具类
 * @author huangjianqin
 * @date 2023/6/1
 */
public final class Serializations {
    private Serializations() {
    }

    /**
     * 根据serialization code查找{@link Serialization}实现
     * @param code  serialization code
     * @return  {@link Serialization}实例
     */
    public static Serialization getByCode(int code) {
        Serialization serialization = ExtensionLoader.getExtension(Serialization.class, code);
        if (Objects.isNull(serialization)) {
            throw new CodecException("can not find Serialization with code " + code);
        }

        return serialization;
    }

    /**
     * 判断code为{@code  code}的序列化实现是否存在当前classpath
     *
     * @param code serialization code
     * @return true表示在当前classpath找到{@code  code}对应的序列化实现
     */
    public static boolean isSerializationExists(int code) {
        Serialization serialization = ExtensionLoader.getExtension(Serialization.class, code);
        return Objects.nonNull(serialization);
    }

    /**
     * 判断name为{@code  name}的序列化实现是否存在当前classpath
     *
     * @param name serialization name
     * @return true表示在当前classpath找到{@code  code}对应的序列化实现
     */
    public static boolean isSerializationExists(String name) {
        Serialization serialization = ExtensionLoader.getExtension(Serialization.class, name);
        return Objects.nonNull(serialization);
    }
}
