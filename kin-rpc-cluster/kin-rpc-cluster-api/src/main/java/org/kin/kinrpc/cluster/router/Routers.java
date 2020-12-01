package org.kin.kinrpc.cluster.router;

import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;

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

    private static String getKey(String className) {
        int index = className.indexOf(Router.class.getSimpleName());
        if (index > 0) {
            className = className.substring(0, index);
        }

        return className.toLowerCase();
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
                String key = getKey(claxx.getSimpleName());
                if (routerCache.containsKey(key)) {
                    throw new RouterConflictException(claxx, routerCache.get(key).getClass());
                }
                try {
                    Router router = claxx.newInstance();
                    routerCache.put(key, router);
                } catch (Exception e) {
                    throw new RpcCallErrorException(e);
                }
            }
        }

        //通过spi机制加载自定义的Router
        Iterator<Router> customRouters = RpcServiceLoader.LOADER.iterator(Router.class);
        while (customRouters.hasNext()) {
            Router router = customRouters.next();
            Class<? extends Router> claxx = router.getClass();
            String key = getKey(claxx.getSimpleName());
            if (routerCache.containsKey(key)) {
                throw new RouterConflictException(claxx, routerCache.get(key).getClass());
            }

            routerCache.put(key, router);
        }

        ROUTER_CACHE = routerCache;
    }

    /**
     * 根据Router name 获取Router instance
     */
    public static Router getRouter(String type) {
        return ROUTER_CACHE.get(type);
    }

    /**
     * 根据Router class 返回Router name, 如果没有加载到, 则主动重新加载
     */
    public static synchronized String getOrLoadRouter(Class<? extends Router> routerClass) {
        String key = getKey(routerClass.getSimpleName());
        if (ROUTER_CACHE.containsKey(key)) {
            //已加载
            return key;
        }

        Map<String, Router> routerCache = new HashMap<>(ROUTER_CACHE);
        //未加载
        try {
            Router router = routerClass.newInstance();
            routerCache.put(key, router);
        } catch (Exception e) {
            throw new RpcCallErrorException(e);
        }

        ROUTER_CACHE = Collections.unmodifiableMap(routerCache);

        return key;
    }
}
