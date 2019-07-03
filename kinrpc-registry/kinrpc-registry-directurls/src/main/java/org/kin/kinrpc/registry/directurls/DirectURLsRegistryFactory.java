package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.serializer.SerializerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        SerializerType serializerType = SerializerType.getByName(url.getParam(Constants.SERIALIZE_KEY));

        List<String> hostAndPorts = new ArrayList<>();
        for(String one: address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)){
            hostAndPorts.add(one);
        }

        try {
            Registry registry = registryCache.get(address, () -> new DirectURLsRegistry(hostAndPorts, serializerType));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error("", e);
        }

        return null;
    }
}
