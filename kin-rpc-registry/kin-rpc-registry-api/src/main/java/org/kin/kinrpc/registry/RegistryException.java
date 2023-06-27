package org.kin.kinrpc.registry;

import org.kin.kinrpc.RpcException;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public class RegistryException extends RpcException {
    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryException(Throwable cause) {
        super(cause);
    }
}
