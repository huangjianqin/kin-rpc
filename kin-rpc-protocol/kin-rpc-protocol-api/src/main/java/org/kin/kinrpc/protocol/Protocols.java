package org.kin.kinrpc.protocol;

import org.kin.framework.utils.ExtensionLoader;

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
