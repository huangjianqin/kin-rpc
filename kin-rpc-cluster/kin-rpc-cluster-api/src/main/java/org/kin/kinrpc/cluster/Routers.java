package org.kin.kinrpc.cluster;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Routers {
    /** key-> class name, value -> Router instance */
    private static volatile Map<String, Router> ROUTER_CACHE = Collections.emptyMap();

    static {
        initLoad();
    }

    private Routers() {
    }

    /**
     * 缓存通过SPI加载的实例
     */
    private static void initLoad() {
        Map<String, Router> routerCache = new HashMap<>();

        //通过spi机制加载自定义的LoadBalance
        List<Router> routers = RpcServiceLoader.LOADER.getExtensions(Router.class);
        for (Router router : routers) {
            Class<? extends Router> claxx = router.getClass();
            String prefixName = ClassUtils.getPrefixName(claxx, Router.class);
            if (StringUtils.isNotBlank(prefixName)) {
                if (routerCache.containsKey(prefixName)) {
                    throw new RouterConflictException(claxx, routerCache.get(prefixName).getClass());
                }

                routerCache.put(prefixName, router);
            }
            String className = claxx.getName();

            if (routerCache.containsKey(className)) {
                throw new RouterConflictException(claxx, routerCache.get(className).getClass());
            }

            routerCache.put(className, router);
        }

        ROUTER_CACHE = Collections.unmodifiableMap(routerCache);

    }

    /**
     * 尝试加载指定类型的Router
     */
    private static synchronized Router loadNew(String type) {
        if (ROUTER_CACHE.containsKey(type)) {
            return ROUTER_CACHE.get(type);
        }

        Router router = null;
        Map<String, Router> routerCache = new HashMap<>(ROUTER_CACHE);
        //加载内部提供的LoadBalance
        Set<Class<? extends Router>> classes = ClassUtils.getSubClass(Router.class.getPackage().getName(), Router.class, true);
        if (classes.size() > 0) {
            for (Class<? extends Router> claxx : classes) {
                String prefixName = ClassUtils.getPrefixName(claxx, Router.class);
                String className = claxx.getName();

                if (!type.equals(prefixName) && !type.equals(className)) {
                    continue;
                }

                router = ClassUtils.instance(claxx);
                if (StringUtils.isNotBlank(prefixName)) {
                    if (routerCache.containsKey(prefixName)) {
                        throw new RouterConflictException(claxx, routerCache.get(prefixName).getClass());
                    }

                    routerCache.put(prefixName, router);
                }

                if (routerCache.containsKey(className)) {
                    throw new RouterConflictException(claxx, routerCache.get(className).getClass());
                }

                routerCache.put(className, router);
            }
        }
        ROUTER_CACHE = Collections.unmodifiableMap(routerCache);

        return router;
    }

    /**
     * 根据Router name 获取Router instance
     */
    public static Router getRouter(String type) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("Router type is blank");
        }

        if (ROUTER_CACHE.containsKey(type)) {
            return ROUTER_CACHE.get(type);
        } else {
            Router router = loadNew(type);
            if (Objects.nonNull(router)) {
                return router;
            }
        }

        throw new IllegalArgumentException(String.format("unknown Router or unable to load Router '%s'", type));
    }
}
