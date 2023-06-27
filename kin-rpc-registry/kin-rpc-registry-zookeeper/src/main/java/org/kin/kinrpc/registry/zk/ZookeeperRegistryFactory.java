package org.kin.kinrpc.registry.zk;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryFactory;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
@Extension("zk")
public class ZookeeperRegistryFactory implements RegistryFactory {
    @Override
    public Registry create(RegistryConfig config) {
        return new ZookeeperRegistry(config);
    }
}
