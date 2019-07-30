package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryFactory implements RegistryFactory{
    protected static final Logger log = LoggerFactory.getLogger(AbstractRegistryFactory.class);
    protected static final Cache<String, Registry> REGISTRY_CACHE = CacheBuilder.newBuilder().build();

    @Override
    public void close(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        Registry registry = REGISTRY_CACHE.getIfPresent(address);

        if(registry.release()){
            REGISTRY_CACHE.invalidate(address);
            registry.destroy();
        }
    }
}
