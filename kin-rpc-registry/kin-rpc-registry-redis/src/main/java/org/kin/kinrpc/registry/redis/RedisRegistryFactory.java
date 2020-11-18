package org.kin.kinrpc.registry.redis;

import org.kin.framework.log.LoggerOprs;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.ExecutionException;

/**
 * redis注册中心工厂
 *
 * @author huangjianqin
 * @date 2020/8/12
 */
public class RedisRegistryFactory extends AbstractRegistryFactory implements LoggerOprs {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new RedisRegistry(url));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
