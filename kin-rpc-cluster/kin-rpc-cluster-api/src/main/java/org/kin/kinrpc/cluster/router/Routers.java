package org.kin.kinrpc.cluster.router;

import org.kin.framework.utils.ClassUtils;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Routers {
    /** key-> class name, value -> Router instance */
    private static volatile Map<String, Router> ROUTER_CACHE = Collections.emptyMap();

    static {
        load();
    }

    private Routers() {
    }

    /**
     * 加载Router
     */
    private static void load() {
        Map<String, Router> routerCache = new HashMap<>();
        //加载内部提供的Router
        Set<Class<? extends Router>> classes = ClassUtils.getSubClass(Router.class.getPackage().getName(), Router.class, true);
        if (classes.size() > 0) {
            for (Class<? extends Router> claxx : classes) {
                String className = claxx.getSimpleName().toLowerCase();
                if (routerCache.containsKey(className)) {
                    throw new RouterConflictException(claxx, routerCache.get(className).getClass());
                }
                try {
                    Router router = claxx.newInstance();
                    routerCache.put(className, router);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //通过spi机制加载自定义的Router
        ServiceLoader<Router> serviceLoader = ServiceLoader.load(Router.class);
        Iterator<Router> customRouters = serviceLoader.iterator();
        while (customRouters.hasNext()) {
            Router router = customRouters.next();
            Class<? extends Router> claxx = router.getClass();
            String className = claxx.getSimpleName().toLowerCase();
            if (routerCache.containsKey(className)) {
                throw new RouterConflictException(claxx, routerCache.get(className).getClass());
            }

            routerCache.put(className, router);
        }

        ROUTER_CACHE = routerCache;
    }

    /**
     * 根据Router name 获取Router instance
     */
    public static Router getRouter(String type) {
        return ROUTER_CACHE.get(type);
    }
}
