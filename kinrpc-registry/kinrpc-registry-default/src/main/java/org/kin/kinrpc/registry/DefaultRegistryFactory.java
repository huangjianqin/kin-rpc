package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.serializer.SerializerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DefaultRegistryFactory extends AbstractRegistryFactory{
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        SerializerType serializerType = SerializerType.getByName(url.getParam(Constants.SERIALIZE_KEY));

        List<HostAndPort> hostAndPorts = new ArrayList<>();
        for(String one: address.split(Constants.DEFAULT_REGISTRY_URL_SPLITOR)){
            hostAndPorts.add(HostAndPort.fromString(one));
        }

        try {
            Registry registry = registryCache.get(address, () -> new DefaultRegistry(hostAndPorts, serializerType));
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error("", e);
        }

        return null;
    }
}