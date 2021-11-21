package org.kin.kinrpc.cluster;

import org.kin.framework.utils.AbstractExtensionCache;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public final class Routers extends AbstractExtensionCache<String, Router> {
    //单例
    public static final Routers INSTANCE = new Routers();

    public Routers() {
        super(RpcServiceLoader.LOADER);
    }

    @Override
    protected String[] keys(Router router) {
        Class<? extends Router> claxx = router.getClass();
        return new String[]{ClassUtils.getPrefixName(claxx, Router.class), claxx.getName(), claxx.getSimpleName()};
    }
}