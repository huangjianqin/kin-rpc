package org.kin.kinrpc.registry.zookeeper;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.serializer.Serializers;

import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        int sessionTimeout = Integer.parseInt(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        String serializerType = url.getParam(Constants.SERIALIZE_KEY);
        boolean compression = Boolean.parseBoolean(url.getParam(Constants.COMPRESSION_KEY));

        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializers.getSerializer(serializerType), "unvalid serializer type: [" + serializerType + "]");

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new ZookeeperRegistry(address, sessionTimeout, serializerType, compression));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
