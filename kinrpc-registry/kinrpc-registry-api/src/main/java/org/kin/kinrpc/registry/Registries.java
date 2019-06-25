package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Registries {
    private static final Cache<String, RegistryFactory> registryFactories = CacheBuilder.newBuilder().build();

    private Registries() {
    }

    private static RegistryFactory getRegistryFactory(URL url){
        String registryType = url.getParam(Constants.REGISTRY).toLowerCase();

        try {
            RegistryFactory registryFactory = registryFactories.get(registryType, () -> {
                Set<Class<RegistryFactory>> classes = ClassUtils.getSubClass("org.kin.kinrpc.registry", RegistryFactory.class, true);
                if (classes.size() > 0) {
                    for (Class<RegistryFactory> claxx : classes) {
                        String className = claxx.getSimpleName().toLowerCase();
                        if (className.startsWith(registryType)) {
                            return claxx.newInstance();
                        }
                    }
                }

                return null;
            });

            return registryFactory;
        } catch (ExecutionException e) {
            ExceptionUtils.log(e);
        }

        throw new IllegalStateException("init registry error >>>" + registryType);
    }

    public static synchronized Registry getRegistry(URL url){
        RegistryFactory registryFactory = getRegistryFactory(url);
        return registryFactory.getRegistry(url);
    }

    public static synchronized void closeRegistry(URL url){
        RegistryFactory registryFactory = getRegistryFactory(url);
        registryFactory.close(url);
    }
}
