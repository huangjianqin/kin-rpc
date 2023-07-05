package org.kin.kinrpc.registry;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class RegistryFactoryNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 8203221139466106072L;

    public RegistryFactoryNotFoundException(String type) {
        super(String.format("can not find %s registry factory", type));
    }
}
