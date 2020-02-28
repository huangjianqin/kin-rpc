package org.kin.kinrpc.registry.zookeeper2;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.serializer.Serializers;

import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class Zookeeper2RegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        int sessionTimeout = Integer.parseInt(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        boolean compression = Boolean.parseBoolean(url.getParam(Constants.COMPRESSION_KEY));

        String serializerType = url.getParam(Constants.SERIALIZE_KEY);
        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializers.getSerializer(serializerType), "unvalid serializer type: [" + serializerType + "]");

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new Zookeeper2Registry(address, sessionTimeout, serializerType, compression));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
