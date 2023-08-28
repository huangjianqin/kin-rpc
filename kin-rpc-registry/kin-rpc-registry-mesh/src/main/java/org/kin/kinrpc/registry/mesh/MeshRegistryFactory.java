package org.kin.kinrpc.registry.mesh;

import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryFactory;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
public class MeshRegistryFactory implements RegistryFactory {
    @Override
    public Registry create(RegistryConfig config) {
        return new MeshRegistry(config);
    }
}