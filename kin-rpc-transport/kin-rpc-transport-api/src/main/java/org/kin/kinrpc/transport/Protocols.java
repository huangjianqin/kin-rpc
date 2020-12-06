package org.kin.kinrpc.transport;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public class Protocols {
    private Protocols() {
    }

    /**
     * 根据Protocol name 获取Protocol instance
     */
    public static Protocol getProtocol(String type) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("Protocol type is blank");
        }
        List<Protocol> protocols = RpcServiceLoader.LOADER.getExtensions(Protocol.class);
        for (Protocol protocol : protocols) {
            String simpleName = protocol.getClass().getSimpleName().toLowerCase();
            if (type.equals(protocol.getClass().getName()) ||
                    type.equals(simpleName) ||
                    type.concat(Protocol.class.getSimpleName()).toLowerCase().equals(simpleName)) {
                //扩展service class name | service simple class name | 前缀 + service simple class name
                return protocol;
            }
        }

        throw new IllegalArgumentException(String.format("unknown Protocol or unable to load Protocol '%s'", type));
    }
}
