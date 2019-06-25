package org.kin.kinrpc.registry.zookeeper;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;

import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL);
        String password = url.getParam(Constants.REGISTRY_PASSWORD);
        int sessionTimeout = Integer.valueOf(url.getParam(Constants.SESSION_TIMEOUT));

        try {
            Registry registry = registryCache.get(address, () -> new ZookeeperRegistry(address, password, sessionTimeout));
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            ExceptionUtils.log(e);
        }

        return null;
    }
}
