package org.kin.kinrpc.bootstrap;

import org.kin.framework.JvmCloseCleaner;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/6/30
 */
public final class KinRpcRuntimeContext {
    /** 已发布的服务 */
    private static final CopyOnWriteArraySet<ServiceBootstrap<?>> EXPORTED_SERVICE_BOOTSTRAP = new CopyOnWriteArraySet<>();

    static {
        JvmCloseCleaner.instance().add(KinRpcRuntimeContext::shutdown);
    }

    private KinRpcRuntimeContext() {
    }

    /**
     * kinrpc app shutdown
     */
    private static void shutdown() {
        //服务下线
        for (ServiceBootstrap<?> serviceBootstrap : EXPORTED_SERVICE_BOOTSTRAP) {
            serviceBootstrap.unExport();
        }
    }

    /**
     * 缓存已发布的服务
     *
     * @param bootstrap service bootstrap
     */
    public static void cacheService(ServiceBootstrap<?> bootstrap) {
        EXPORTED_SERVICE_BOOTSTRAP.add(bootstrap);
    }
}
