package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);

        List<Url> urls = Arrays.asList(address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)).stream().map(Url::of).collect(Collectors.toList());

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new DirectURLsRegistry(urls));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
