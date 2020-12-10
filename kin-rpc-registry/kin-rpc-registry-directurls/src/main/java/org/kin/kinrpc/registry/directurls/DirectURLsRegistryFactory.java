package org.kin.kinrpc.registry.directurls;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new DirectURLsRegistry(url));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }
}
