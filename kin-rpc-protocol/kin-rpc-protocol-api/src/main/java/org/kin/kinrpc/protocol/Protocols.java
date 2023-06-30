package org.kin.kinrpc.protocol;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.transport.cmd.CodecException;

import java.util.Objects;

/**
 * 协议相关工具类
 *
 * @author huangjianqin
 * @date 2023/6/29
 */
public final class Protocols {
    private Protocols() {
    }

    /**
     * 根据protocol name查找{@link Protocol}实现
     *
     * @param name protocol name
     * @return {@link Protocol}实例
     */
    public static Protocol getByName(String name) {
        Protocol protocol = ExtensionLoader.getExtension(Protocol.class, name);
        if (Objects.isNull(protocol)) {
            throw new CodecException("can not find Protocol named " + name);
        }

        return protocol;
    }

    /**
     * 判断{@code  name}对应的协议实现是否存在当前classpath
     *
     * @param name protocol name
     * @return true表示在当前classpath找到{@code  name}对应的协议实现
     */
    public static boolean isProtocolExists(String name) {
        Protocol protocol = ExtensionLoader.getExtension(Protocol.class, name);
        return Objects.nonNull(protocol);
    }
}
