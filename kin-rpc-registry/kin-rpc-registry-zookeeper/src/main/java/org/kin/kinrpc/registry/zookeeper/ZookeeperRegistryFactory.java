package org.kin.kinrpc.registry.zookeeper;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.transport.netty.CompressionType;

import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        long sessionTimeout = Long.parseLong(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        int compression = Integer.parseInt(url.getParam(Constants.COMPRESSION_KEY));

        int serializerType = Integer.parseInt(url.getParam(Constants.SERIALIZE_KEY));
        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializers.getSerializer(serializerType), "unvalid serializer type: [" + serializerType + "]");

        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(compressionType, "unvalid compression type: id=" + compression + "");

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new ZookeeperRegistry(address, sessionTimeout, serializerType, compressionType));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
