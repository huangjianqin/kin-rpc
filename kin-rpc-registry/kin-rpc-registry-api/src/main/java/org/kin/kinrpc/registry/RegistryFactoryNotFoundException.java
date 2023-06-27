package org.kin.kinrpc.registry;

import org.kin.kinrpc.RpcException;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class RegistryFactoryNotFoundException extends RpcException {
    private static final long serialVersionUID = 8203221139466106072L;

    public RegistryFactoryNotFoundException(String type) {
        super(String.format("can not find %s registry factory", type));
    }
}
