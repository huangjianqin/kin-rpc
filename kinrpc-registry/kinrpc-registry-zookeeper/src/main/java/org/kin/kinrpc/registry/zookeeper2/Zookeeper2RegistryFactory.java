package org.kin.kinrpc.registry.zookeeper2;

import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.serializer.SerializerType;

import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class Zookeeper2RegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        int sessionTimeout = Integer.valueOf(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        SerializerType serializerType = SerializerType.getByName(url.getParam(Constants.SERIALIZE_KEY));

        try {
            Registry registry = registryCache.get(address, () -> new Zookeeper2Registry(address, sessionTimeout, serializerType));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error("", e);
        }

        return null;
    }
}
