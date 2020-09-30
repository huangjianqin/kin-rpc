package org.kin.kinrpc.registry.directurls;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.transport.netty.CompressionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        int serializerType = Integer.parseInt(url.getParam(Constants.SERIALIZE_KEY));
        int compression = Integer.parseInt(url.getParam(Constants.COMPRESSION_KEY));

        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializers.getSerializer(serializerType), "unvalid serializer type: [" + serializerType + "]");

        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(compressionType, "unvalid compression type: id=" + compression + "");

        List<String> hostAndPorts = new ArrayList<>(Arrays.asList(address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)));

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new DirectURLsRegistry(hostAndPorts, serializerType, compressionType));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
