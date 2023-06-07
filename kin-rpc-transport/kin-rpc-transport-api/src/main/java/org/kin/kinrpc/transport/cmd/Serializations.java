package org.kin.kinrpc.transport.cmd;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.transport.CodecException;
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
    public static Serialization getByCode(int code){
        Serialization serialization = ExtensionLoader.getExtension(Serialization.class, code);
        if (Objects.isNull(serialization)) {
            throw new CodecException("can not find Serialization with code " + code);
        }

        return serialization;
    }
}
