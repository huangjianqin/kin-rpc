package org.kin.kinrpc.cluster.router;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class Routers {
    private static final Logger log = LoggerFactory.getLogger(Routers.class);
    private static final Cache<String, Router> ROUTER_CACHE = CacheBuilder.newBuilder().build();

    private Routers(){
    }

    public static Router getRouter(String type){
        //从整个classpath寻找Router子类
        type = type.toLowerCase();
        try {
            String routerName = (type + Router.class.getSimpleName()).toLowerCase();
            Router router = ROUTER_CACHE.get(type, () -> {
                Set<Class<Router>> classes = ClassUtils.getSubClass("", Router.class, true);
                if (classes.size() > 0) {
                    for (Class<Router> claxx : classes) {
                        String className = claxx.getSimpleName().toLowerCase();
                        if (className.equals(routerName)) {
                            return claxx.newInstance();
                        }
                    }
                }

                return null;
            });

            return router;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        throw new IllegalStateException("init router error >>>" + type);
    }
}
