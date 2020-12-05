package org.kin.kinrpc.registry.zookeeper;

import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        long sessionTimeout = url.getLongParam(Constants.SESSION_TIMEOUT_KEY);

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new ZookeeperRegistry(url));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
