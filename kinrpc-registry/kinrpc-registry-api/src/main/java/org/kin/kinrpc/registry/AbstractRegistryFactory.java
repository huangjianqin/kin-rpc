package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryFactory implements RegistryFactory{
    protected Cache<String, Registry> registryCache = CacheBuilder.newBuilder().build();

    @Override
    public void close(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL);
        Registry registry = registryCache.getIfPresent(address);

        if(registry.release()){
            registryCache.invalidate(address);
            registry.destroy();
        }
    }
}
