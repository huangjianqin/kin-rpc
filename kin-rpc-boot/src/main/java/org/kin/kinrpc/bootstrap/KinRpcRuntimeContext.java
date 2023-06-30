package org.kin.kinrpc.bootstrap;

import org.kin.framework.JvmCloseCleaner;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/6/30
 */
public final class KinRpcRuntimeContext {
    /** 已发布的服务 */
    private static final CopyOnWriteArraySet<ServiceBootstrap<?>> SERVICE_BOOTSTRAP_SET = new CopyOnWriteArraySet<>();
    /** 已引用的服务代理 */
    private static final CopyOnWriteArraySet<ReferenceBootstrap<?>> REFERENCE_BOOTSTRAP_SET = new CopyOnWriteArraySet<>();

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
        for (ServiceBootstrap<?> serviceBootstrap : SERVICE_BOOTSTRAP_SET) {
            serviceBootstrap.unExport();
        }

        //注销服务引用
        for (ReferenceBootstrap<?> referenceBootstrap : REFERENCE_BOOTSTRAP_SET) {
            referenceBootstrap.unRefer();
        }
    }

    /**
     * 缓存已发布的服务
     *
     * @param bootstrap service bootstrap
     */
    public static void cacheService(ServiceBootstrap<?> bootstrap) {
        SERVICE_BOOTSTRAP_SET.add(bootstrap);
    }

    /**
     * 缓存已引用的服务代理
     *
     * @param bootstrap reference bootstrap
     */
    public static void cacheReference(ReferenceBootstrap<?> bootstrap) {
        REFERENCE_BOOTSTRAP_SET.add(bootstrap);
    }
}
