package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Registries {
    private static final Logger log = LoggerFactory.getLogger(Registries.class);
    private static final Cache<String, RegistryFactory> REGISTRY_FACTORY_CACHE = CacheBuilder.newBuilder().build();

    private Registries() {
    }

    private static RegistryFactory getRegistryFactory(Url url) {
        String registryType = url.getParam(Constants.REGISTRY_KEY);
        if (StringUtils.isBlank(registryType)) {
            return null;
        }

        //从整个classpath寻找RegistryFactory子类
        try {
            return REGISTRY_FACTORY_CACHE.get(registryType, () -> {
                Set<Class<? extends RegistryFactory>> classes = ClassUtils.getSubClass(RegistryFactory.class.getPackage().getName(), RegistryFactory.class, true);
                if (classes.size() > 0) {
                    for (Class<? extends RegistryFactory> claxx : classes) {
                        String simpleName = claxx.getSimpleName();
                        if (registryType.equals(claxx.getName()) ||
                                registryType.equalsIgnoreCase(simpleName) ||
                                registryType.concat(RegistryFactory.class.getSimpleName()).equalsIgnoreCase(simpleName)) {
                            //扩展service class name | service simple class name | 前缀 + service simple class name
                            return claxx.newInstance();
                        }
                    }
                }

                return null;
            });
        } catch (ExecutionException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("init registry error >>>" + registryType);
    }

    public static synchronized Registry getRegistry(Url url) {
        RegistryFactory registryFactory = getRegistryFactory(url);
        if (registryFactory != null) {
            return registryFactory.getRegistry(url);
        }
        return null;
    }

    public static synchronized void closeRegistry(Url url) {
        RegistryFactory registryFactory = getRegistryFactory(url);
        if (registryFactory != null) {
            registryFactory.close(url);
        }
    }
}
