package org.kin.kinrpc.registry.direct;

import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryFactory;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectRegistryFactory implements RegistryFactory {
    @Override
    public Registry create(RegistryConfig config) {
        return new DirectRegistry(config);
    }
}
